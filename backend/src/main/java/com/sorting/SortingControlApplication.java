package com.sorting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SortingControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(SortingControlApplication.class, args);
    }
}
