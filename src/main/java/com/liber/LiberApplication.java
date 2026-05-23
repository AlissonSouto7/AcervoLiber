package com.liber;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan("com.liber.config")
@EnableScheduling
public class LiberApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiberApplication.class, args);
    }
}
