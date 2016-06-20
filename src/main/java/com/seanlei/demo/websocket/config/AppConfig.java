package com.seanlei.demo.websocket.config;

import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.stereotype.Controller;

/**
 * Created by Sean Lei on 6/20/16.
 */
@Configuration
@ComponentScan(basePackages = {"com.seanlei.demo.websocket.webapp"}, excludeFilters = @ComponentScan.Filter(value =
        Controller.class, type = FilterType.ANNOTATION))
@PropertySources(value = @PropertySource("classpath:config.properties"))
public class AppConfig {
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
