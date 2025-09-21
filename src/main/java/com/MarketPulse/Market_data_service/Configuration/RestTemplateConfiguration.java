package com.MarketPulse.Market_data_service.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableScheduling
public class RestTemplateConfiguration {
    
    @Bean
    public RestTemplate restTemplate(){
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(60000);
        RestTemplate restTemplate = new RestTemplate(factory);

        // Add byte array converter for handling gzipped content
        restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());

        // Add interceptor for gzip handling
        restTemplate.getInterceptors().add(((request, body, execution) -> {
            request.getHeaders().add("Accept-Encoding", "gzip, deflate");
            request.getHeaders().add("User-Agent", "MarketDataService/1.0");
            return execution.execute(request, body);
        }));

        return restTemplate;
    }

}
