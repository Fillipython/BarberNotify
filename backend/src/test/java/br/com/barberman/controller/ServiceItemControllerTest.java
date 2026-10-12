package br.com.barberman.controller;

import br.com.barberman.dto.ServiceItemResponseDto;
import br.com.barberman.service.ServiceItemService;
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

@WebMvcTest(ServiceItemController.class)
class ServiceItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServiceItemService serviceItemService;

    @Test
    @DisplayName("GET /api/services deve retornar 200 OK com lista de servicos")
    void testListActiveServices() throws Exception {
        ServiceItemResponseDto s1 = ServiceItemResponseDto.builder()
                .id(UUID.randomUUID())
                .name("Corte")
                .priceCents(3000)
                .durationMinutes(30)
                .build();

        when(serviceItemService.findAllActive()).thenReturn(List.of(s1));

        mockMvc.perform(get("/services")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Corte"))
                .andExpect(jsonPath("$[0].priceCents").value(3000));
    }
}
