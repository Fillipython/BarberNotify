package br.com.barberman.controller;

import br.com.barberman.dto.ClientRequestDto;
import br.com.barberman.dto.ClientResponseDto;
import br.com.barberman.service.ClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClientService clientService;

    @Test
    @DisplayName("GET /clients/search deve retornar 200 OK quando cliente for encontrado")
    void testSearchByPhone_Found() throws Exception {
        UUID id = UUID.randomUUID();
        ClientResponseDto client = ClientResponseDto.builder()
                .id(id)
                .name("Carlos Alberto")
                .phone("11988887777")
                .email("carlos@teste.com")
                .notificationsEnabled(true)
                .build();

        when(clientService.findByPhone("11988887777")).thenReturn(Optional.of(client));

        mockMvc.perform(get("/clients/search")
                        .param("phone", "11988887777")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Carlos Alberto"));
    }

    @Test
    @DisplayName("GET /clients/search deve retornar 404 Not Found quando cliente nao for encontrado")
    void testSearchByPhone_NotFound() throws Exception {
        when(clientService.findByPhone("11988887777")).thenReturn(Optional.empty());

        mockMvc.perform(get("/clients/search")
                        .param("phone", "11988887777")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /clients com query param deve retornar 200 OK")
    void testGetByPhoneParam_Success() throws Exception {
        UUID id = UUID.randomUUID();
        ClientResponseDto client = ClientResponseDto.builder()
                .id(id)
                .name("Carlos Alberto")
                .phone("11988887777")
                .build();

        when(clientService.findByPhone("11988887777")).thenReturn(Optional.of(client));

        mockMvc.perform(get("/clients")
                        .param("phone", "11988887777")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Carlos Alberto"));
    }

    @Test
    @DisplayName("POST /clients deve retornar 200 OK ao criar ou atualizar cliente valido")
    void testCreateOrUpdate_Valid() throws Exception {
        UUID id = UUID.randomUUID();
        ClientRequestDto request = ClientRequestDto.builder()
                .name("Marcos Silva")
                .phone("11999991111")
                .email("marcos@teste.com")
                .notificationsEnabled(true)
                .build();

        ClientResponseDto response = ClientResponseDto.builder()
                .id(id)
                .name("Marcos Silva")
                .phone("11999991111")
                .email("marcos@teste.com")
                .notificationsEnabled(true)
                .build();

        when(clientService.createOrUpdate(any(ClientRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Marcos Silva"));
    }

    @Test
    @DisplayName("POST /clients deve retornar 422 ao enviar dados invalidos")
    void testCreateOrUpdate_InvalidPayload() throws Exception {
        ClientRequestDto invalidRequest = ClientRequestDto.builder()
                .name("") // Invalido: em branco
                .phone("12") // Invalido: menor que 10
                .build();

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.phone").exists());
    }
}
