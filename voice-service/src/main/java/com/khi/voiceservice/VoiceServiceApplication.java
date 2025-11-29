package com.khi.voiceservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class VoiceServiceApplication {
    //test
    public static void main(String[] args) {
        SpringApplication.run(VoiceServiceApplication.class, args);
    }

}
