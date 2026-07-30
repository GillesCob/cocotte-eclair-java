package com.gillescobigo.cocotteeclair.service;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

// Appel REST direct à l'API Resend plutôt qu'un SDK tiers, pour ne dépendre que d'un
// endpoint HTTP documenté et stable (POST /emails, Bearer token) au lieu d'une
// bibliothèque Java dont la surface d'API exacte n'a pas été vérifiée.
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final URI RESEND_ENDPOINT = URI.create("https://api.resend.com/emails");

    private final String apiKey;
    private final String fromAddress;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmailService(
            @Value("${app.resend.api-key}") String apiKey,
            @Value("${app.resend.from-address}") String fromAddress
    ) {
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
    }

    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        // Instanciation paresseuse du client HTTP : uniquement au moment de l'envoi,
        // jamais au démarrage du serveur.
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        String html = "<p>Vous avez demandé la réinitialisation de votre mot de passe CocotteEclair.</p>"
                + "<p><a href=\"" + resetUrl + "\">Cliquez ici pour choisir un nouveau mot de passe</a></p>"
                + "<p>Ce lien expire dans 15 minutes. Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.</p>";

        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "from", fromAddress,
                    "to", toEmail,
                    "subject", "Réinitialisation de votre mot de passe CocotteEclair",
                    "html", html
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(RESEND_ENDPOINT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Echec envoi email Resend, status {}", response.statusCode());
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de reset", e);
        }
    }
}
