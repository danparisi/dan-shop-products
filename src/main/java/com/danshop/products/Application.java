package com.danshop.products;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@Slf4j
@EnableFeignClients
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        try {
            SpringApplication
                    .run(Application.class, args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(Environment environment) {
        String serviceName = environment.getProperty("spring.application.name");

        /* Exposing application name to Prometheus, so that it's shown on Grafana */
        return registry -> registry.config().commonTags("application", serviceName);
    }

}
