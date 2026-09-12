package com.edu.recommendation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@MapperScan("com.edu.recommendation.mapper")
@SpringBootApplication(scanBasePackages = {"com.edu.recommendation", "com.edu.common"})
public class RecommendationServiceApplication {
    public static void main(String[] args) { SpringApplication.run(RecommendationServiceApplication.class, args); }
}
