package com.example.conferenceservice.common.config;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // RestClient.Builder는 baseUrl 등을 설정하며 자기 자신을 변형해 반환하므로,
    // 싱글톤으로 두면 여러 클라이언트가 같은 Builder 인스턴스를 공유해 설정이 서로 덮어써질 수 있다.
    // Spring Boot의 자동구성 Builder 빈도 동일한 이유로 prototype 스코프를 사용한다.
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(500);
        factory.setReadTimeout(1000);

        return RestClient.builder()
                .requestFactory(factory);
    }
}
