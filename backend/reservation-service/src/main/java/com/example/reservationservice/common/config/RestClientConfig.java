package com.example.reservationservice.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;


@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(500); // 연결 Timeout (ms)
        factory.setReadTimeout(1000); // 연결 Timeout (ms)

        return RestClient.builder()
                .requestFactory(factory);
    }
}
