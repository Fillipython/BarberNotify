package br.com.caiosbarber.controller;

import br.com.caiosbarber.dto.ServiceItemResponseDto;
import br.com.caiosbarber.service.ServiceItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/services")
public class ServiceItemController {

    private final ServiceItemService serviceItemService;

    public ServiceItemController(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    @GetMapping
    public ResponseEntity<List<ServiceItemResponseDto>> listActiveServices() {
        return ResponseEntity.ok(serviceItemService.findAllActive());
    }
}
