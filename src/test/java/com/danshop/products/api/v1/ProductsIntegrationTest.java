package com.danshop.products.api.v1;

import com.danshop.products.client.ProductInventoryDTO;
import com.danshop.products.persistency.model.ProductEntity;
import com.danshop.products.persistency.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.danshop.products.api.v1.ProductsController.BASE_ENDPOINT_PRODUCTS;
import static com.danshop.products.api.v1.ProductsIntegrationTest.LoadBalancerTestConfiguration;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Optional.empty;
import static java.util.function.Function.identity;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static reactor.core.publisher.Flux.just;

@EmbeddedKafka
@Import(LoadBalancerTestConfiguration.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ProductsIntegrationTest {
    private static final int AN_INNER_QUANTITY_1 = 1;
    private static final int AN_INNER_QUANTITY_3 = 3;
    private static final int AN_INNER_QUANTITY_5 = 5;
    private static final String A_MISSING_CODE = "missing-code";
    private static final String A_PRODUCT_CODE = "a-product-code";
    private static final String A_PRODUCT_NAME = "a-product-name";
    private static final String A_NEW_PRODUCT_CODE = "a-new-product-code";
    private static final String A_NEW_PRODUCT_NAME = "a-new-product-name";
    private static final String ENDPOINT_ALL_PRODUCTS = BASE_ENDPOINT_PRODUCTS;
    private static final String ENDPOINT_PRODUCT_CODE = BASE_ENDPOINT_PRODUCTS + "/{code}";

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private WireMockServer wireMockServer;
    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void beforeEach() {
        wireMockServer.resetAll();
        productRepository.deleteAll();
    }

    @Test
    @SneakyThrows
    void shouldGetProduct() {
        ProductEntity product = storeNewProduct();
        String productCode = product.getCode();
        stubProductInventoryCall(productCode, AN_INNER_QUANTITY_5);

        ResponseEntity<ProductDTO> response = testRestTemplate
                .getForEntity(ENDPOINT_PRODUCT_CODE, ProductDTO.class, productCode);

        ProductDTO actual = response.getBody();
        assertNotNull(actual);
        assertEquals(OK, response.getStatusCode());
        assertEquals(productCode, actual.getCode());
        assertEquals(AN_INNER_QUANTITY_5, actual.getQuantity());
    }

    @Test
    @SneakyThrows
    void shouldGetAllProducts() {
        ProductEntity product1 = storeNewProduct();
        ProductEntity product2 = storeNewProduct();
        ProductEntity product3 = storeNewProduct();
        stubProductInventoryCall(product1.getCode(), AN_INNER_QUANTITY_1);
        stubProductInventoryCall(product2.getCode(), AN_INNER_QUANTITY_3);
        stubProductInventoryCall(product3.getCode(), AN_INNER_QUANTITY_5);

        ResponseEntity<ProductDTO[]> response = testRestTemplate
                .getForEntity(ENDPOINT_ALL_PRODUCTS, ProductDTO[].class);

        assertEquals(OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, ProductDTO> products = stream(response.getBody()).collect(Collectors.toMap(ProductDTO::getCode, identity()));
        assertEquals(3, products.size());
        assertEquals(products.get(product1.getCode()).getQuantity(), AN_INNER_QUANTITY_1);
        assertEquals(products.get(product2.getCode()).getQuantity(), AN_INNER_QUANTITY_3);
        assertEquals(products.get(product3.getCode()).getQuantity(), AN_INNER_QUANTITY_5);
    }

    @Test
    @SneakyThrows
    void shouldReturnNoContentIfProductNotFound() {
        ResponseEntity<ProductInventoryDTO> response = testRestTemplate
                .getForEntity(ENDPOINT_PRODUCT_CODE, ProductInventoryDTO.class, A_MISSING_CODE);

        assertEquals(NO_CONTENT, response.getStatusCode());
    }

    @Test
    @SneakyThrows
    void shouldAddNewProduct() {
        ProductDTO newProductDTO = createProductDTO(A_PRODUCT_CODE, A_PRODUCT_NAME, AN_INNER_QUANTITY_3);
        stubUpdateInventoryCall(A_PRODUCT_CODE, AN_INNER_QUANTITY_3);

        ResponseEntity<ProductDTO> result = testRestTemplate
                .postForEntity(BASE_ENDPOINT_PRODUCTS, newProductDTO, ProductDTO.class);

        assertEquals(CREATED, result.getStatusCode());
        verifyResponse(newProductDTO, result);
        verifyDatabaseProduct(newProductDTO, A_PRODUCT_CODE);
        verifyUpdateInventoryCall(A_PRODUCT_CODE, AN_INNER_QUANTITY_3);
    }

    @Test
    @SneakyThrows
    void shouldNotAddNewProductIfInvalidCode() {
        ProductDTO newProductDTO = createProductDTO(null, A_PRODUCT_NAME, AN_INNER_QUANTITY_3);

        ResponseEntity<Object> result = testRestTemplate
                .postForEntity(BASE_ENDPOINT_PRODUCTS, newProductDTO, Object.class);

        assertEquals(BAD_REQUEST, result.getStatusCode());
        verifyUpdateInventoryCallNotExecuted(newProductDTO.getCode());
    }

    @Test
    @SneakyThrows
    void shouldNotAddNewProductIfInvalidName() {
        ProductDTO newProductDTO = createProductDTO(A_NEW_PRODUCT_CODE, null, AN_INNER_QUANTITY_3);

        ResponseEntity<Object> result = testRestTemplate
                .postForEntity(BASE_ENDPOINT_PRODUCTS, newProductDTO, Object.class);

        assertEquals(BAD_REQUEST, result.getStatusCode());
        verifyUpdateInventoryCallNotExecuted(newProductDTO.getCode());
    }

    @Test
    @SneakyThrows
    void shouldUpdateExistingProduct() {
        ProductEntity existingProduct = storeNewProduct();
        String existingProductCode = existingProduct.getCode();
        UpdateProductDTO updateProductDTO = createUpdateProductDTO(A_NEW_PRODUCT_NAME, AN_INNER_QUANTITY_5);
        stubUpdateInventoryCall(existingProductCode, AN_INNER_QUANTITY_5);

        ResponseEntity<ProductDTO> result = testRestTemplate
                .exchange(ENDPOINT_PRODUCT_CODE, PUT, new HttpEntity<>(updateProductDTO), ProductDTO.class, existingProductCode);

        assertEquals(OK, result.getStatusCode());
        verifyResponse(updateProductDTO, result);
        verifyDatabaseProduct(updateProductDTO, existingProductCode);
        verifyUpdateInventoryCall(existingProductCode, AN_INNER_QUANTITY_5);
    }

    @Test
    @SneakyThrows
    void shouldNotUpdateExistingProductIfInvalidName() {
        ProductEntity existingProduct = storeNewProduct();
        String existingProductCode = existingProduct.getCode();
        UpdateProductDTO updateProductDTO = createUpdateProductDTO(null, AN_INNER_QUANTITY_5);

        ResponseEntity<String> result = testRestTemplate
                .exchange(ENDPOINT_PRODUCT_CODE, PUT, new HttpEntity<>(updateProductDTO), String.class, existingProductCode);

        assertNotNull(result.getBody());
        assertEquals(BAD_REQUEST, result.getStatusCode());
        verifyUpdateInventoryCallNotExecuted(existingProductCode);
    }

    @Test
    @SneakyThrows
    void shouldDeleteProductInventory() {
        ProductEntity existingProduct = storeNewProduct();
        String productCode = existingProduct.getCode();
        stubDeleteInventoryCall(productCode);

        testRestTemplate.delete(ENDPOINT_PRODUCT_CODE, productCode);

        verifyDeleteInventoryCall(productCode);
        assertEquals(empty(), productRepository.findByCode(productCode));
    }

    private void verifyDatabaseProduct(ProductDTO expected, String productCode) {
        ProductEntity productEntity = retrieveMandatoryProductByCode(productCode);

        assertEquals(expected.getCode(), productEntity.getCode());
        assertEquals(expected.getName(), productEntity.getName());
    }

    private void verifyDatabaseProduct(UpdateProductDTO expected, String productCode) {
        ProductEntity productEntity = retrieveMandatoryProductByCode(productCode);

        assertEquals(productCode, productEntity.getCode());
        assertEquals(expected.getName(), productEntity.getName());
    }

    private static void verifyResponse(ProductDTO expected, ResponseEntity<ProductDTO> result) {
        ProductDTO productDTO = result.getBody();
        assertNotNull(productDTO);

        assertEquals(expected.getCode(), productDTO.getCode());
        assertEquals(expected.getName(), productDTO.getName());
        assertEquals(expected.getQuantity(), productDTO.getQuantity());
    }

    private static void verifyResponse(UpdateProductDTO expected, ResponseEntity<ProductDTO> result) {
        ProductDTO productDTO = result.getBody();
        assertNotNull(productDTO);

        assertEquals(expected.getName(), productDTO.getName());
        assertEquals(expected.getQuantity(), productDTO.getQuantity());
    }

    private ProductEntity retrieveMandatoryProductByCode(String code) {
        return productRepository
                .findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(format("Expected Product with code [%s] not found", code)));
    }

    private ProductEntity storeNewProduct() {
        return productRepository.save(
                ProductEntity.builder()
                        .code(randomAlphabetic(5))
                        .name(random(20)).build());
    }

    private static ProductDTO createProductDTO(String code, String name, int quantity) {
        return ProductDTO.builder()
                .code(code)
                .name(name)
                .quantity(quantity).build();
    }

    private static UpdateProductDTO createUpdateProductDTO(String name, int quantity) {
        return UpdateProductDTO.builder()
                .name(name)
                .quantity(quantity).build();
    }

    private void stubProductInventoryCall(String productCode, int innerQuantity) throws JsonProcessingException {
        wireMockServer
                .stubFor(get("/v1/products/" + productCode)
                        .willReturn(aResponse()
                                .withStatus(OK.value())
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withBody(objectMapper
                                        .writeValueAsString(ProductInventoryDTO.builder()
                                                .code(productCode)
                                                .innerQuantity(innerQuantity).build()))));
    }

    private void verifyUpdateInventoryCall(String code, int innerQuantity) {
        wireMockServer.
                verify(putRequestedFor(urlEqualTo("/v1/products/" + code))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withRequestBody(matchingJsonPath("$.inner_quantity", equalTo(String.valueOf(innerQuantity)))));
    }

    private void verifyUpdateInventoryCallNotExecuted(String code) {
        wireMockServer.
                verify(exactly(0), putRequestedFor(urlEqualTo("/v1/products/" + code)));
    }

    private void verifyDeleteInventoryCall(String code) {
        wireMockServer.
                verify(deleteRequestedFor(urlEqualTo("/v1/products/" + code)));
    }

    private void stubUpdateInventoryCall(String productCode, int innerQuantity) throws JsonProcessingException {
        wireMockServer
                .stubFor(put("/v1/products/" + productCode)
                        .willReturn(aResponse()
                                .withStatus(OK.value())
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withBody(objectMapper
                                        .writeValueAsString(ProductInventoryDTO.builder()
                                                .code(productCode)
                                                .innerQuantity(innerQuantity).build()))));
    }

    private void stubDeleteInventoryCall(String productCode) {
        wireMockServer
                .stubFor(delete("/v1/products/" + productCode)
                        .willReturn(aResponse()
                                .withStatus(OK.value())));
    }

    @TestConfiguration
    public static class LoadBalancerTestConfiguration {
        @Bean(initMethod = "start", destroyMethod = "stop")
        public WireMockServer mockBooksService() {
            return new WireMockServer(0);
        }

        @Bean
        @Primary
        public ServiceInstanceListSupplier serviceInstanceListSupplier(final WireMockServer wireMockServer) {
            return new ServiceInstanceListSupplier() {
                @Override
                public String getServiceId() {
                    return "";
                }

                @Override
                public Flux<List<ServiceInstance>> get() {
                    return just(List.of(new DefaultServiceInstance("", "", "localhost", wireMockServer.port(), false)));
                }
            };
        }
    }
}
