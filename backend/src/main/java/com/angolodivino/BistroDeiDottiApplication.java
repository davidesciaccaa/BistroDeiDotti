package com.angolodivino;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BistroDeiDottiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BistroDeiDottiApplication.class, args);
    }
}
