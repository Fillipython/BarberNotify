package br.com.barberman;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BarberManApplicationTests {

    @Test
    @DisplayName("Contexto do Spring Boot deve carregar com sucesso com base H2 de testes")
    void contextLoads() {
    }
}
