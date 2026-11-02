
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

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiService {

    private static final Logger log =
            LoggerFactory.getLogger(GeminiService.class);

    private static final Pattern RISK_PATTERN =
            Pattern.compile("(?im)^RIESGO_SCORE\\s*:\\s*(1|2|3|4|5|SIN_EVALUAR)\\s*$");

    private static final Pattern REASON_PATTERN =
            Pattern.compile("(?im)^RIESGO_MOTIVO\\s*:\\s*(.*)$");

    private static final Pattern PROBLEM_PATTERN =
            Pattern.compile("(?im)^PROBLEMA_PRINCIPAL\\s*:\\s*(.*)$");

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
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

                PREPARA UN INFORME CON ESTAS SECCIONES:

                1. DIAGNÓSTICO GENERAL
                Explica la situación de la reputación del comercio.

                2. SENTIMIENTO GENERAL
                Clasifica el sentimiento como positivo, neutral o negativo
                y justifica brevemente la clasificación.

                3. FORTALEZAS
                Identifica los aspectos positivos respaldados por
                la información disponible.

                4. PROBLEMAS RECURRENTES
                Identifica las dificultades mencionadas en las reseñas.
                No presentes un problema como recurrente si no hay
                evidencia suficiente.

                5. OPORTUNIDADES DE MEJORA
                Propone acciones concretas y realistas.

                6. RECOMENDACIONES
                Indica qué debería priorizar el comercio.

                7. RESPUESTAS A CLIENTES
                Redacta dos respuestas profesionales y empáticas
                para comentarios críticos. Si no hay reseñas críticas,
                aclara que no hay material suficiente para responder
                a comentarios concretos.

                8. PRIORIDAD COMERCIAL
                Indica si la situación representa una oportunidad
                de intervención baja, media o alta, y explica por qué.

                9. CLASIFICACIÓN DE RIESGO
                Asigna un único puntaje entero según la gravedad de
                los problemas de reputación demostrados por los datos:
                1 = Mínimo: reputación favorable o sin señales negativas.
                2 = Bajo: señales negativas leves y aisladas.
                3 = Medio: problemas moderados o varias señales negativas.
                4 = Alto: problemas importantes respaldados por evidencia.
                5 = Crítico: problemas graves y reiterados respaldados
                    por evidencia.

                Si la información no permite evaluar razonablemente
                el riesgo, utiliza SIN_EVALUAR. No inventes datos.
                Si no hay textos de reseñas, puedes hacer una evaluación
                preliminar con la calificación y cantidad de reseñas
                únicamente si esos datos son suficientes. Aclara esa
                limitación en el informe.

                AL FINAL DEL INFORME, ESCRIBE OBLIGATORIAMENTE
                ESTAS TRES LÍNEAS, RESPETANDO EXACTAMENTE EL FORMATO:

                RIESGO_SCORE: 1, 2, 3, 4, 5 o SIN_EVALUAR
                RIESGO_MOTIVO: explicación breve del puntaje
                PROBLEMA_PRINCIPAL: problema principal o Sin evidencia suficiente

                REGLAS IMPORTANTES:
                - No inventes reseñas, opiniones ni datos.
                - Diferencia los datos del perfil de las opiniones
                  expresadas en reseñas.
                - No afirmes que un problema es recurrente sin evidencia.
                - No confundas prioridad comercial con gravedad del riesgo.
                - Respondé en español, con títulos y secciones claras.
                - Las tres líneas finales deben aparecer separadas,
                  sin formato Markdown y sin texto después de ellas.
                """.formatted(
                businessName == null ? "Sin nombre" : businessName,
                reviewsText == null || reviewsText.isBlank()
                        ? "Sin información adicional"
                        : reviewsText
        );

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));

        Map<String, Object> generationConfig = Map.of(
                "temperature", 0.3,
                "maxOutputTokens", 2048
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
                        "(?im)^RIESGO_SCORE\\s*:\\s*(1|2|3|4|5|SIN_EVALUAR)\\s*$",
                        ""
                )
                .replaceAll("(?im)^RIESGO_MOTIVO\\s*:\\s*.*$", "")
                .replaceAll("(?im)^PROBLEMA_PRINCIPAL\\s*:\\s*.*$", "")
                .trim();

        if (riskScore == null
                && (riskReason == null || riskReason.isBlank())) {
            riskReason = "Gemini no proporcionó evidencia suficiente para clasificar el riesgo.";
        }

        if (mainProblem == null || mainProblem.isBlank()) {
            mainProblem = "Sin evidencia suficiente";
        }

        return new AnalysisResult(
                report,
                riskScore,
                riskReason,
                mainProblem
        );
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
