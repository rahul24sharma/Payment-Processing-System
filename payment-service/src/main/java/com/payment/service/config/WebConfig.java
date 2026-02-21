package com.payment.service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// CORS is handled by the API Gateway (globalcors). Do not add CORS headers here
// or the response will contain duplicate Access-Control-Allow-Origin values.
@Configuration
public class WebConfig implements WebMvcConfigurer {
}