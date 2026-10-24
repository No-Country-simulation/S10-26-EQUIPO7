
package com.repusense.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key:${GEMINI_API_KEY:clave_demo_desarrollo}}")
    private String apiKey;

    @Value("${gemini.api.url:${GEMINI_API_URL:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String analyzeBusinessReviews(String businessName, String reviewsText) {
        String url = apiUrl + "?key=" + apiKey;

        // Construir el prompt estructurado para RepuSense
        String prompt = String.format(
            "Actúa como un experto en reputación online y consultor de marketing. " +
            "Analiza las siguientes reseñas del comercio '%s':\n\n%s\n\n" +
            "Por favor, genera un JSON con:\n" +
            "1. Diagnóstico de problemas principales (atención, precios, tiempos, etc.).\n" +
            "2. Sentimiento general (Positivo, Neutral, Negativo).\n" +
            "3. Dos opciones de respuestas profesionales y empáticas que el dueño pueda copiar y publicar para responder a las reseñas críticas.",
            businessName, reviewsText
        );

        // Estructura de payload que espera la API de Gemini
        Map<String, Object> contentPart = new HashMap<>();
        contentPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(contentPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            return "{\"error\": \"Error al conectar con la API de Gemini: " + e.getMessage() + "\"}";
        }
    }
}