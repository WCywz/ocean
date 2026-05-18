package com.ocean;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.ocean.mapper")
@EnableScheduling
public class OceanApplication {
    public static void main(String[] args) {
        SpringApplication.run(OceanApplication.class, args);
    }
}
