package br.com.barberman.service;

import br.com.barberman.model.Appointment;
import br.com.barberman.model.NotificationLog;
import br.com.barberman.repository.NotificationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppNotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final ObjectMapper objectMapper;

    @Value("${whatsapp.enabled:false}")
    private boolean enabled;

    @Value("${whatsapp.api.url:http://evolution_api:8080}")
    private String apiUrl;

    @Value("${whatsapp.api.key:barberman_secret_key_123}")
    private String apiKey;

    @Value("${whatsapp.instance:barber_man}")
    private String instance;

    @Value("${whatsapp.sender.phone:}")
    private String senderPhone;

    @Value("${whatsapp.notify.owner:false}")
    private boolean notifyOwner;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Envia mensagem de confirmacao de agendamento via WhatsApp de forma assincrona
     */
    public void sendAppointmentConfirmation(Appointment appointment) {
        if (!enabled) {
            log.info("Notificacoes via WhatsApp estao desabilitadas nas configuracoes.");
            return;
        }

        if (appointment == null || appointment.getClient() == null || appointment.getClient().getPhone() == null) {
            log.warn("Nao foi possivel enviar WhatsApp: dados de agendamento ou telefone do cliente ausentes.");
            return;
        }

        // Execucao assincrona para nunca travar a resposta do cliente no site
        CompletableFuture.runAsync(() -> {
            try {
                String clientPhone = formatBrazilianPhone(appointment.getClient().getPhone());
                if (clientPhone == null) {
                    log.warn("Telefone do cliente invalido para envio de WhatsApp: {}", appointment.getClient().getPhone());
                    return;
                }

                String clientMessage = buildClientConfirmationMessage(appointment);
                boolean clientSent = sendTextMessage(clientPhone, clientMessage);

                // Registra o log no banco de dados
                saveNotificationLog(appointment, clientPhone, "Agendamento Confirmado - Barber Man", clientMessage, clientSent);

                // Notifica o proprietario/barbeiro se configurado
                if (notifyOwner && senderPhone != null && !senderPhone.isBlank()) {
                    String ownerPhone = formatBrazilianPhone(senderPhone);
                    if (ownerPhone != null && !ownerPhone.equals(clientPhone)) {
                        String ownerMessage = buildOwnerNotificationMessage(appointment);
                        sendTextMessage(ownerPhone, ownerMessage);
                    }
                }
            } catch (Exception e) {
                log.error("Erro inesperado ao processar notificacao WhatsApp: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Dispara requisicao POST para o endpoint /message/sendText/{instance} da Evolution API
     */
    public boolean sendTextMessage(String recipientPhone, String text) {
        try {
            String baseUrl = apiUrl.replaceAll("/+$", "");
            String targetUrl = String.format("%s/message/sendText/%s", baseUrl, instance);

            Map<String, Object> body = new HashMap<>();
            body.put("number", recipientPhone);
            body.put("text", text);

            String jsonPayload = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .header("apikey", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("WhatsApp enviado com sucesso para {}. Status HTTP: {}", recipientPhone, response.statusCode());
                return true;
            } else {
                log.warn("Evolution API retornou status {} ao enviar para {}: {}", response.statusCode(), recipientPhone, response.body());
                return false;
            }
        } catch (Exception e) {
            log.warn("Falha de conexao com a Evolution API ao enviar WhatsApp para {}: {}", recipientPhone, e.getMessage());
            return false;
        }
    }

    /**
     * Consulta status da instancia na Evolution API
     */
    public Map<String, Object> checkStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", enabled);
        result.put("apiUrl", apiUrl);
        result.put("instance", instance);

        if (!enabled) {
            result.put("status", "DISABLED");
            return result;
        }

        try {
            String baseUrl = apiUrl.replaceAll("/+$", "");
            String targetUrl = String.format("%s/instance/connectionState/%s", baseUrl, instance);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("apikey", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            result.put("statusCode", response.statusCode());
            result.put("response", response.body());
            result.put("online", response.statusCode() == 200);
        } catch (Exception e) {
            result.put("online", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Cria a instancia na Evolution API se ela ainda nao existir
     */
    public Map<String, Object> createInstance() {
        Map<String, Object> result = new HashMap<>();
        try {
            String baseUrl = apiUrl.replaceAll("/+$", "");
            String targetUrl = String.format("%s/instance/create", baseUrl);

            Map<String, Object> body = new HashMap<>();
            body.put("instanceName", instance);
            body.put("token", apiKey);
            body.put("qrcode", true);

            String jsonPayload = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("apikey", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            result.put("statusCode", response.statusCode());
            result.put("response", response.body());
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * Obtem o QR Code para conectar o WhatsApp escaneando no celular
     */
    public Map<String, Object> getConnectQrCode() {
        Map<String, Object> result = new HashMap<>();
        try {
            String baseUrl = apiUrl.replaceAll("/+$", "");
            String targetUrl = String.format("%s/instance/connect/%s", baseUrl, instance);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("apikey", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            result.put("statusCode", response.statusCode());
            result.put("response", response.body());
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * Formata mensagem amigavel e profissional para o cliente
     */
    private String buildClientConfirmationMessage(Appointment appointment) {
        String dataFormatada = appointment.getScheduledAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));
        String barbeiroNome = appointment.getBarber() != null ? appointment.getBarber().getName() : "Barbeiro Barber Man";
        String clienteNome = appointment.getClient() != null ? appointment.getClient().getName() : "Cliente";

        String servicos = appointment.getServices() != null && !appointment.getServices().isEmpty()
                ? appointment.getServices().stream()
                    .map(s -> s.getService().getName())
                    .collect(Collectors.joining(", "))
                : "Corte Masculino";

        int duracaoMins = appointment.getTotalDurationMinutes() != null ? appointment.getTotalDurationMinutes() : 30;
        String duracaoLabel = duracaoMins >= 60
                ? (duracaoMins / 60) + "h" + (duracaoMins % 60 > 0 ? " " + (duracaoMins % 60) + "min" : "")
                : duracaoMins + " min";

        int precoCents = appointment.getTotalPriceCents() != null ? appointment.getTotalPriceCents() : 0;
        String precoLabel = String.format("R$ %.2f", precoCents / 100.0).replace('.', ',');

        return String.format(
                "💈 *Barber Man - Agendamento Confirmado!* 💈\n\n" +
                "Olá, *%s*! Seu agendamento foi realizado com sucesso.\n\n" +
                "📅 *Data:* %s\n" +
                "✂️ *Profissional:* %s\n" +
                "💈 *Serviço(s):* %s\n" +
                "⏱️ *Tempo estimado:* %s\n" +
                "💰 *Total:* %s\n\n" +
                "Te esperamos na barbearia! Caso precise remarcar ou cancelar, basta responder a esta mensagem ou acessar o site.",
                clienteNome, dataFormatada, barbeiroNome, servicos, duracaoLabel, precoLabel
        );
    }

    /**
     * Formata mensagem de aviso para o barbeiro / proprietario
     */
    private String buildOwnerNotificationMessage(Appointment appointment) {
        String dataFormatada = appointment.getScheduledAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));
        String clienteNome = appointment.getClient() != null ? appointment.getClient().getName() : "Cliente";
        String clienteTel = appointment.getClient() != null ? appointment.getClient().getPhone() : "-";
        String barbeiroNome = appointment.getBarber() != null ? appointment.getBarber().getName() : "Barbeiro";

        int precoCents = appointment.getTotalPriceCents() != null ? appointment.getTotalPriceCents() : 0;
        String precoLabel = String.format("R$ %.2f", precoCents / 100.0).replace('.', ',');

        return String.format(
                "🔔 *Novo Agendamento Confirmado!*\n\n" +
                "👤 *Cliente:* %s (%s)\n" +
                "✂️ *Profissional:* %s\n" +
                "📅 *Data:* %s\n" +
                "💰 *Valor:* %s\n",
                clienteNome, clienteTel, barbeiroNome, dataFormatada, precoLabel
        );
    }

    /**
     * Normaliza numeros de telefone brasileiros para DDI 55 + DDD + Numero
     */
    public String formatBrazilianPhone(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("\\D", "");

        if (digits.length() < 10) {
            return null;
        }

        // Se ja tiver DDI 55
        if (digits.startsWith("55") && (digits.length() == 12 || digits.length() == 13)) {
            return digits;
        }

        // Se tiver apenas DDD + Numero (10 ou 11 digitos)
        if (digits.length() == 10 || digits.length() == 11) {
            return "55" + digits;
        }

        return digits;
    }

    private void saveNotificationLog(Appointment appointment, String recipient, String title, String message, boolean success) {
        try {
            notificationLogRepository.save(NotificationLog.builder()
                    .client(appointment.getClient())
                    .appointment(appointment)
                    .channel("WHATSAPP")
                    .recipient(recipient)
                    .title(title)
                    .message(message)
                    .status(success ? "SENT" : "FAILED")
                    .build());
        } catch (Exception e) {
            log.warn("Nao foi possivel salvar NotificationLog: {}", e.getMessage());
        }
    }
}
