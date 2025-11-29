package com.khi.ragservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class RagServiceApplication {
    //test
    public static void main(String[] args) {
        SpringApplication.run(RagServiceApplication.class, args);
    }

}
