package br.com.caiosbarber.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotDto {
    private String time;
    private Boolean available;
}
