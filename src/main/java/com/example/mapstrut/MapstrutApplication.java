package com.example.mapstrut;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class MapstrutApplication {

    public static void main(String[] args) {
        SpringApplication.run(MapstrutApplication.class, args);
    }

}
