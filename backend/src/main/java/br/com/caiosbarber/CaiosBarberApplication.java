package br.com.caiosbarber;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CaiosBarberApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaiosBarberApplication.class, args);
    }
}
