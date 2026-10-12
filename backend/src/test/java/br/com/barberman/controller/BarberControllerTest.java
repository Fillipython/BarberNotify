package br.com.barberman.controller;

import br.com.barberman.dto.BarberResponseDto;
import br.com.barberman.service.BarberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BarberController.class)
class BarberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BarberService barberService;

    @Test
    @DisplayName("GET /api/barbers deve retornar 200 OK com lista de barbeiros ativos")
    void testListActiveBarbers() throws Exception {
        BarberResponseDto b1 = BarberResponseDto.builder()
                .id(UUID.randomUUID())
                .name("Carlos Silva")
                .nickname("Mestre Carlos")
                .phone("11988880001")
                .email("carlos@barberman.com.br")
                .isAdmin(true)
                .build();

        when(barberService.findAllActive()).thenReturn(List.of(b1));

        mockMvc.perform(get("/barbers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Carlos Silva"))
                .andExpect(jsonPath("$[0].isAdmin").value(true));
    }
}
