package com.arana.pokedex.infrastructure.pokeapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableCaching
public class PokeApiConfig {

    @Bean
    RestClient pokeApiRestClient(RestClient.Builder builder,
                                 @Value("${pokeapi.base-url}") String baseUrl,
                                 @Value("${pokeapi.connect-timeout}") Duration connectTimeout,
                                 @Value("${pokeapi.read-timeout}") Duration readTimeout) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(connectTimeout)
                .withReadTimeout(readTimeout);
        return builder
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "pokedex-api/1.0")
                .build();
    }
}
