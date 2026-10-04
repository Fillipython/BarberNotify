package br.com.barberman.dto;

import br.com.barberman.model.ServiceItem;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceItemResponseDto {
    private UUID id;
    private String name;
    private String description;
    private Integer priceCents;
    private Integer durationMinutes;
    private String durationLabel;
    private String imageUrl;

    public static ServiceItemResponseDto fromEntity(ServiceItem item) {
        if (item == null) return null;
        int mins = item.getDurationMinutes();
        String label;
        if (mins >= 60) {
            int h = mins / 60;
            int m = mins % 60;
            label = m > 0 ? h + "hr " + m + "min" : h + "hr";
        } else {
            label = mins + "min";
        }

        return ServiceItemResponseDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .priceCents(item.getPriceCents())
                .durationMinutes(item.getDurationMinutes())
                .durationLabel(label)
                .imageUrl(item.getImageUrl())
                .build();
    }
}
