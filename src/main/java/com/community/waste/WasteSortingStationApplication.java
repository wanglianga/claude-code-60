package com.community.waste;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WasteSortingStationApplication {

    public static void main(String[] args) {
        SpringApplication.run(WasteSortingStationApplication.class, args);
    }
}
