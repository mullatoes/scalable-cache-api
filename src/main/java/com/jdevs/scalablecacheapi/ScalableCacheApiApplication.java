package com.jdevs.scalablecacheapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class ScalableCacheApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScalableCacheApiApplication.class, args);
    }

}
