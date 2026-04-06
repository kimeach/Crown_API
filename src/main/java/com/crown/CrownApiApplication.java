package com.crown;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CrownApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrownApiApplication.class, args);
    }
}
