package com.arana.gateway.web;

import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Spring Cloud Gateway's {@code globalcors} only applies to <em>routed</em> requests, but the
 * token endpoint and JWKS are served by the gateway itself: without this filter the browser
 * blocked the SPA's login call. One WebFilter applies the same {@code globalcors} settings to
 * every path (routed or not), so allowed origins are configured in a single place. Routed
 * responses don't get duplicate headers: Spring's CorsProcessor skips responses that already
 * carry Access-Control-Allow-Origin.
 */
@Configuration
public class CorsConfig {

    @Bean
    CorsWebFilter corsWebFilter(GlobalCorsProperties globalCors) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        globalCors.getCorsConfigurations().forEach(source::registerCorsConfiguration);
        return new CorsWebFilter(source);
    }
}
