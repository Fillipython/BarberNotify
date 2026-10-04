package br.com.barberman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BarberManApplication {

    public static void main(String[] args) {
        SpringApplication.run(BarberManApplication.class, args);
    }
}
