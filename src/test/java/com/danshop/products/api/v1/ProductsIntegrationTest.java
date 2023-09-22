package com.danshop.products.api.v1;

import com.danshop.products.client.ProductInventoryDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.SneakyThrows;
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
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import reactor.core.publisher.Flux;

import java.util.List;

import static com.danshop.products.api.v1.ProductsController.BASE_ENDPOINT_PRODUCTS;
import static com.danshop.products.api.v1.ProductsIntegrationTest.LoadBalancerTestConfiguration;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static reactor.core.publisher.Flux.just;

@EmbeddedKafka//(ports = 9092)
@Import(LoadBalancerTestConfiguration.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ProductsIntegrationTest {
    private static final int AN_INNER_QUANTITY = 5;
    private static final String A_PRODUCT_CODE_123 = "123";
    private static final String A_PRODUCT_CODE_456 = "456";
    private static final String A_PRODUCT_CODE_789 = "789";
    private static final String A_MISSING_CODE = "missing-code";
    private static final String ENDPOINT_ALL_PRODUCTS = BASE_ENDPOINT_PRODUCTS;
    private static final String ENDPOINT_PRODUCT = BASE_ENDPOINT_PRODUCTS + "/{code}";
    private static final ProductInventoryDTO A_PRODUCT_INVENTORY_DTO = ProductInventoryDTO.builder()
            .code(A_PRODUCT_CODE_123)
            .innerQuantity(AN_INNER_QUANTITY).build();

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private WireMockServer wireMockServer;
    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    @SneakyThrows
    void shouldGetProduct() {
        wireMockServer
                .stubFor(get("/v1/products/" + A_PRODUCT_CODE_123)
                        .willReturn(aResponse()
                                .withStatus(OK.value())
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsString(A_PRODUCT_INVENTORY_DTO))));

        ResponseEntity<ProductDTO> response = testRestTemplate
                .getForEntity(ENDPOINT_PRODUCT, ProductDTO.class, A_PRODUCT_CODE_123);

        ProductDTO actual = response.getBody();
        assertNotNull(actual);
        assertEquals(OK, response.getStatusCode());
        assertEquals(A_PRODUCT_CODE_123, actual.getCode());
        assertEquals(AN_INNER_QUANTITY, actual.getQuantity());
    }

    @Test
    @SneakyThrows
    void shouldGetAllProducts() {
        wireMockServer
                .stubFor(get("/v1/products/" + A_PRODUCT_CODE_456)
                        .willReturn(aResponse()
                                .withStatus(NO_CONTENT.value())));
        wireMockServer
                .stubFor(get("/v1/products/" + A_PRODUCT_CODE_789)
                        .willReturn(aResponse()
                                .withStatus(NO_CONTENT.value())));
        wireMockServer
                .stubFor(get("/v1/products/" + A_PRODUCT_CODE_123)
                        .willReturn(aResponse()
                                .withStatus(OK.value())
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsString(A_PRODUCT_INVENTORY_DTO))));

        ResponseEntity<ProductDTO[]> response = testRestTemplate
                .getForEntity(ENDPOINT_ALL_PRODUCTS, ProductDTO[].class);

        List<ProductDTO> actual = asList(response.getBody());
        assertEquals(OK, response.getStatusCode());
        assertEquals(3, actual.size());
    }

    @Test
    @SneakyThrows
    void shouldReturnNoContentIfProductNotFound() {
        ResponseEntity<ProductInventoryDTO> response = testRestTemplate
                .getForEntity(ENDPOINT_PRODUCT, ProductInventoryDTO.class, A_MISSING_CODE);

        assertEquals(NO_CONTENT, response.getStatusCode());
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
