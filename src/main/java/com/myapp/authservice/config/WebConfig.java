package com.myapp.authservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * Web Configuration for handling reverse proxy headers
 *
 * When behind Nginx/Load Balancer, the application needs to trust
 * X-Forwarded-* headers to get the real client IP and protocol.
 */
@Configuration
public class WebConfig {

    @Value("${server.forward-headers-strategy:NONE}")
    private String forwardHeadersStrategy;

    /**
     * Register ForwardedHeaderFilter to process X-Forwarded-* headers
     * This ensures getRemoteAddr() returns the real client IP, not the proxy IP
     */
    @Bean
    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        FilterRegistrationBean<ForwardedHeaderFilter> filterRegBean = new FilterRegistrationBean<>();
        filterRegBean.setFilter(new ForwardedHeaderFilter());
        filterRegBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filterRegBean;
    }
}
