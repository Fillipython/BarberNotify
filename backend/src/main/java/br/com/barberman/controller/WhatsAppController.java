package br.com.barberman.controller;

import br.com.barberman.service.WhatsAppNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final WhatsAppNotificationService whatsAppNotificationService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(whatsAppNotificationService.checkStatus());
    }

    @GetMapping("/connect")
    public ResponseEntity<Map<String, Object>> getQrCode() {
        return ResponseEntity.ok(whatsAppNotificationService.getConnectQrCode());
    }

    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initInstance() {
        return ResponseEntity.ok(whatsAppNotificationService.createInstance());
    }
}
