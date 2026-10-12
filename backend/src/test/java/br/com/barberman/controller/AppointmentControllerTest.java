package br.com.barberman.controller;

import br.com.barberman.dto.AppointmentRequestDto;
import br.com.barberman.dto.AppointmentResponseDto;
import br.com.barberman.dto.TimeSlotDto;
import br.com.barberman.service.AppointmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppointmentService appointmentService;

    @Test
    @DisplayName("POST /appointments deve criar agendamento e retornar 200 OK")
    void testCreateAppointment_Success() throws Exception {
        UUID aptId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID srvId = UUID.randomUUID();

        AppointmentRequestDto request = AppointmentRequestDto.builder()
                .clientId(clientId)
                .barberId(barberId)
                .serviceIds(List.of(srvId))
                .scheduledAt(OffsetDateTime.parse("2026-10-15T10:00:00-03:00"))
                .clientNotes("Corte simples")
                .build();

        AppointmentResponseDto response = AppointmentResponseDto.builder()
                .id(aptId)
                .barberName("Carlos Silva")
                .clientName("João")
                .totalPriceCents(3500)
                .totalDurationMinutes(30)
                .status("CONFIRMED")
                .build();

        when(appointmentService.createAppointment(any(AppointmentRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aptId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /appointments com corpo invalido deve retornar 422")
    void testCreateAppointment_Invalid() throws Exception {
        AppointmentRequestDto invalidRequest = AppointmentRequestDto.builder().build();

        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("GET /appointments com clientId deve retornar lista com 200 OK")
    void testListByClient_WithClientId() throws Exception {
        UUID clientId = UUID.randomUUID();
        when(appointmentService.findByClientId(clientId)).thenReturn(List.of());

        mockMvc.perform(get("/appointments")
                        .param("clientId", clientId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(appointmentService).findByClientId(clientId);
    }

    @Test
    @DisplayName("GET /appointments com phone deve retornar lista com 200 OK")
    void testListByClient_WithPhone() throws Exception {
        when(appointmentService.findByClientPhone("11999998888")).thenReturn(List.of());

        mockMvc.perform(get("/appointments")
                        .param("phone", "11999998888")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(appointmentService).findByClientPhone("11999998888");
    }

    @Test
    @DisplayName("GET /appointments sem parametros deve retornar 400 Bad Request")
    void testListByClient_NoParams() throws Exception {
        mockMvc.perform(get("/appointments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /appointments/slots deve retornar lista de slots com 200 OK")
    void testGetAvailableSlots() throws Exception {
        UUID barberId = UUID.randomUUID();
        when(appointmentService.getAvailableSlots(eq(barberId), eq("2026-10-15"), eq(30)))
                .thenReturn(List.of(new TimeSlotDto("09:00", true)));

        mockMvc.perform(get("/appointments/slots")
                        .param("barberId", barberId.toString())
                        .param("date", "2026-10-15")
                        .param("duration", "30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].time").value("09:00"))
                .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    @DisplayName("PATCH /appointments/{id}/cancel deve cancelar e retornar 204 No Content")
    void testCancelAppointment() throws Exception {
        UUID aptId = UUID.randomUUID();
        doNothing().when(appointmentService).cancelAppointment(aptId);

        mockMvc.perform(patch("/appointments/{id}/cancel", aptId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(appointmentService).cancelAppointment(aptId);
    }
}
