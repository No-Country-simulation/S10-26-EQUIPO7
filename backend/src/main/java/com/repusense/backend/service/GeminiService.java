
package com.repusense.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiService {

    private static final Logger log =
            LoggerFactory.getLogger(GeminiService.class);

    private static final Pattern RISK_PATTERN =
            Pattern.compile(
                    "(?im)^\\s*RIESGO_SCORE\\s*:\\s*(SIN_EVALUAR|[1-5])\\b.*$"
            );

    private static final Pattern REASON_PATTERN =
            Pattern.compile(
                    "(?im)^\\s*RIESGO_MOTIVO\\s*:\\s*(.*?)\\s*$"
            );

    private static final Pattern PROBLEM_PATTERN =
            Pattern.compile(
                    "(?im)^\\s*PROBLEMA_PRINCIPAL\\s*:\\s*(.*?)\\s*$"
            );

    private static final Pattern SECTION_PATTERN =
            Pattern.compile(
                    "(?im)^\\s{0,3}#{0,3}\\s*(\\d+)\\.\\s+(.+?)\\s*$"
            );

    @Value("${GEMINI_API_KEY:}")
    private String apiKey;

    @Value("${GEMINI_API_URL:https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiService() {
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(15000);
        factory.setReadTimeout(90000);

        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    public AnalysisResult analyzeBusinessReviews(
            String businessName,
            String reviewsText
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "No está configurada GEMINI_API_KEY."
            );
        }

        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalStateException(
                    "No está configurada la URL de Gemini."
            );
        }

        String prompt = """
                Actúa como especialista en reputación online,
                experiencia del cliente y marketing comercial.

                Analiza el siguiente comercio: %s

                INFORMACIÓN DISPONIBLE:
                %s

                PREPARA UN INFORME CON ESTAS NUEVE SECCIONES.
                Cada sección debe aparecer UNA SOLA VEZ y en este orden.

                1. DIAGNÓSTICO GENERAL
                Describe brevemente la reputación del comercio.

                2. SENTIMIENTO GENERAL
                Clasifica el sentimiento como positivo, neutral o negativo
                y justifica brevemente.

                3. FORTALEZAS
                Indica hasta tres aspectos positivos respaldados por datos.

                4. PROBLEMAS RECURRENTES
                Identifica hasta tres problemas respaldados por reseñas.
                No inventes problemas ni los presentes como recurrentes
                sin evidencia suficiente.

                5. OPORTUNIDADES DE MEJORA
                Propone hasta tres acciones concretas.

                6. RECOMENDACIONES
                Indica hasta tres prioridades para el comercio.

                7. RESPUESTAS A CLIENTES
                Redacta dos respuestas profesionales y empáticas,
                de hasta 35 palabras cada una.
                Si no hay textos de reseñas críticas, aclara que
                no hay material suficiente para responder comentarios concretos.

                8. PRIORIDAD COMERCIAL
                Clasifica la oportunidad como baja, media o alta
                y explica brevemente por qué.

                9. CLASIFICACIÓN DE RIESGO
                Asigna un único puntaje entero:
                1 = Mínimo: reputación favorable o sin señales negativas.
                2 = Bajo: señales negativas leves y aisladas.
                3 = Medio: problemas moderados respaldados por datos.
                4 = Alto: problemas importantes respaldados por evidencia.
                5 = Crítico: problemas graves y reiterados respaldados
                    por evidencia.

                Si no hay información suficiente para evaluar el riesgo,
                utiliza SIN_EVALUAR.

                Si no hay textos de reseñas, puedes realizar una evaluación
                preliminar usando la calificación y cantidad de reseñas,
                únicamente si esos datos permiten una valoración razonable.
                Aclara que se trata de una evaluación preliminar.

                REGLAS DE EXTENSIÓN:
                - El informe completo no debe superar las 400 palabras.
                - Cada sección debe ser breve y concreta.
                - No repitas información ni títulos.
                - No agregues introducciones ni conclusiones adicionales.
                - No inventes reseñas, opiniones ni datos.
                - No atribuyas problemas específicos sin evidencia textual.
                - Diferencia la prioridad comercial del riesgo reputacional.
                - Respondé en español.

                AL FINAL DEL INFORME ESCRIBE ESTAS TRES LÍNEAS:
                RIESGO_SCORE: escribe solamente 1, 2, 3, 4, 5 o SIN_EVALUAR
                RIESGO_MOTIVO: explicación breve del puntaje
                PROBLEMA_PRINCIPAL: problema demostrado o Sin evidencia suficiente

                Las tres líneas deben estar separadas.
                No escribas nada después de PROBLEMA_PRINCIPAL.
                """.formatted(
                businessName == null ? "Sin nombre" : businessName,
                reviewsText == null || reviewsText.isBlank()
                        ? "Sin información adicional"
                        : reviewsText
        );

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));

        Map<String, Object> generationConfig = Map.of(
                "temperature", 0.2,
                "maxOutputTokens", 1200
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(content),
                "generationConfig", generationConfig
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(requestBody, headers);

        long start = System.currentTimeMillis();

        log.info(
                "[Gemini] Iniciando análisis del comercio: {}",
                businessName
        );

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            apiUrl,
                            entity,
                            String.class
                    );

            String responseBody = response.getBody();

            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalStateException(
                        "Gemini respondió sin contenido."
                );
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (!candidates.isArray() || candidates.isEmpty()) {
                String blockReason = root.path("promptFeedback")
                        .path("blockReason")
                        .asText("");

                throw new IllegalStateException(
                        blockReason.isBlank()
                                ? "Gemini no generó un análisis."
                                : "Gemini bloqueó la solicitud: " + blockReason
                );
            }

            JsonNode parts = candidates.get(0)
                    .path("content")
                    .path("parts");

            StringBuilder result = new StringBuilder();

            if (parts.isArray()) {
                for (JsonNode responsePart : parts) {
                    String text = responsePart.path("text").asText("");

                    if (!text.isBlank()) {
                        result.append(text).append("\n");
                    }
                }
            }

            String completeText = result.toString().trim();

            if (completeText.isBlank()) {
                throw new IllegalStateException(
                        "Gemini respondió, pero no devolvió texto."
                );
            }

            AnalysisResult analysisResult = parseAnalysis(completeText);

            long elapsed = System.currentTimeMillis() - start;

            log.info(
                    "[Gemini] Análisis completado. Riesgo: {}. Tiempo: {} ms",
                    analysisResult.riskScore(),
                    elapsed
            );

            return analysisResult;

        } catch (HttpStatusCodeException e) {
            log.error(
                    "[Gemini] Error HTTP {} durante el análisis.",
                    e.getStatusCode()
            );

            throw new IllegalStateException(
                    "Error HTTP de Gemini: "
                            + e.getStatusCode()
                            + ". Verificá la clave API, el modelo "
                            + "y los límites de uso gratuitos."
            );

        } catch (ResourceAccessException e) {
            log.error(
                    "[Gemini] Error de conexión o tiempo de espera."
            );

            throw new IllegalStateException(
                    "No se pudo conectar con Gemini o se agotó el tiempo de espera."
            );

        } catch (Exception e) {
            log.error(
                    "[Gemini] Error interno durante el análisis. Tipo: {}",
                    e.getClass().getSimpleName()
            );

            throw new IllegalStateException(
                    "No se pudo completar el análisis con Gemini. Intente nuevamente."
            );
        }
    }

    private AnalysisResult parseAnalysis(String completeText) {

        Integer riskScore = null;
        String riskReason = null;
        String mainProblem = null;

        Matcher riskMatcher = RISK_PATTERN.matcher(completeText);

        if (riskMatcher.find()) {
            String value = riskMatcher.group(1);

            if (!"SIN_EVALUAR".equalsIgnoreCase(value)) {
                riskScore = Integer.parseInt(value);
            }
        }

        Matcher reasonMatcher = REASON_PATTERN.matcher(completeText);

        if (reasonMatcher.find()) {
            riskReason = cleanValue(reasonMatcher.group(1));
        }

        Matcher problemMatcher = PROBLEM_PATTERN.matcher(completeText);

        if (problemMatcher.find()) {
            mainProblem = cleanValue(problemMatcher.group(1));
        }

        String report = completeText
                .replaceAll(
                        "(?im)^\\s*RIESGO_SCORE\\s*:\\s*.*$",
                        ""
                )
                .replaceAll(
                        "(?im)^\\s*RIESGO_MOTIVO\\s*:\\s*.*$",
                        ""
                )
                .replaceAll(
                        "(?im)^\\s*PROBLEMA_PRINCIPAL\\s*:\\s*.*$",
                        ""
                )
                .trim();

        report = removeDuplicateSections(report);

        if (riskScore == null
                && (riskReason == null || riskReason.isBlank())) {
            riskReason =
                    "Gemini no proporcionó evidencia suficiente para clasificar el riesgo.";
        }

        if (mainProblem == null || mainProblem.isBlank()) {
            mainProblem = "Sin evidencia suficiente";
        }

        if (report.isBlank()) {
            report = "Gemini no generó un informe válido.";
        }

        return new AnalysisResult(
                report,
                riskScore,
                riskReason,
                mainProblem
        );
    }

    private String removeDuplicateSections(String report) {

        String[] lines = report.split("\\R");
        StringBuilder cleaned = new StringBuilder();

        Set<String> seenSections = new HashSet<>();

        boolean skippingDuplicate = false;

        for (String line : lines) {

            Matcher matcher = SECTION_PATTERN.matcher(line);

            if (matcher.matches()) {

                String sectionNumber = matcher.group(1);

                if (seenSections.contains(sectionNumber)) {
                    skippingDuplicate = true;
                    continue;
                }

                seenSections.add(sectionNumber);
                skippingDuplicate = false;
            }

            if (!skippingDuplicate) {
                cleaned.append(line).append("\n");
            }
        }

        return cleaned.toString().trim();
    }

    private String cleanValue(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    public record AnalysisResult(
            String analysis,
            Integer riskScore,
            String riskReason,
            String mainProblem
    ) {
    }
}
