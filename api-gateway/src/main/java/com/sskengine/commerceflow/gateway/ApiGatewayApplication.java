package com.sskengine.commerceflow.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;


@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    RestClient catalogClient(@Value("${clients.catalog.base-url}") String url) {
        return RestClient.create(url);
    }

    @Bean
    RestClient orderClient(@Value("${clients.order.base-url}") String url) {
        return RestClient.create(url);
    }

    @Bean
    RestClient paymentClient(@Value("${clients.payment.base-url}") String url) {
        return RestClient.create(url);
    }
}
