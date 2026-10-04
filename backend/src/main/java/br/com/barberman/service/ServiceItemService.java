package br.com.barberman.service;

import br.com.barberman.dto.ServiceItemResponseDto;
import br.com.barberman.model.ServiceItem;
import br.com.barberman.repository.ServiceItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ServiceItemService {

    private final ServiceItemRepository serviceItemRepository;

    public ServiceItemService(ServiceItemRepository serviceItemRepository) {
        this.serviceItemRepository = serviceItemRepository;
    }

    @Transactional(readOnly = true)
    public List<ServiceItemResponseDto> findAllActive() {
        return serviceItemRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(ServiceItemResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceItem> findEntitiesByIds(List<UUID> ids) {
        List<ServiceItem> items = serviceItemRepository.findByIdInAndIsActiveTrue(ids);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Nenhum servico valido encontrado para os IDs informados");
        }
        return items;
    }
}
