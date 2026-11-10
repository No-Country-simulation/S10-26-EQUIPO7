
package com.repusense.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repusense.backend.model.Business;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GoogleMapsService {

    private static final Logger log =
            LoggerFactory.getLogger(GoogleMapsService.class);

    private static final String API_URL =
            "https://serpapi.com/search.json";

    private static final int MAX_RESULTS = 20;

    private static final double MIN_RATING = 1.0;
    private static final double MAX_RATING = 4.5;
    private static final int MIN_REVIEWS = 10;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public GoogleMapsService(
            @Value("${serpapi.api.key:}") String apiKey) {

        this.apiKey = apiKey;

        var factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(10000);
        factory.setReadTimeout(60000);

        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    public List<Business> fetchBusinessesByCityAndCategory(
            String city, String category) {

        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException(
                    "Ingresá una ciudad.");
        }

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException(
                    "Ingresá un rubro.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar SERPAPI_API_KEY.");
        }

        String cleanCity = city.trim();
        String cleanCategory = category.trim();

        List<Business> businesses = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        int received = 0;
        int missingData = 0;
        int rejectedRating = 0;
        int rejectedReviews = 0;
        int duplicates = 0;

        List<String> queries = List.of(
                cleanCategory + " en " + cleanCity + ", Argentina"
        );

        try {
            for (String query : queries) {

                if (businesses.size() >= MAX_RESULTS) {
                    break;
                }

                var uri = UriComponentsBuilder
                        .fromUriString(API_URL)
                        .queryParam("engine", "google_maps")
                        .queryParam("type", "search")
                        .queryParam("q", query)
                        .queryParam("hl", "es")
                        .queryParam("gl", "ar")
                        .queryParam("api_key", apiKey)
                        .build()
                        .encode()
                        .toUri();

                log.info("[SerpApi] Iniciando búsqueda de rubro: {}",
                        cleanCategory);

                var response = restTemplate.getForEntity(
                        uri, String.class);

                if (response.getBody() == null
                        || response.getBody().isBlank()) {
                    throw new IllegalStateException(
                            "SerpApi devolvió una respuesta vacía.");
                }

                JsonNode root = objectMapper.readTree(
                        response.getBody());

                if (root == null || !root.isObject()) {
                    throw new IllegalStateException(
                            "SerpApi devolvió una respuesta inválida.");
                }

                if (root.has("error")) {
                    throw new IllegalStateException(
                            "SerpApi no pudo completar la búsqueda.");
                }

                JsonNode places = root.path("local_results");

                if (!places.isArray()) {
                    throw new IllegalStateException(
                            "La respuesta de SerpApi no contiene resultados válidos.");
                }

                log.info("[SerpApi] HTTP: {}",
                        response.getStatusCode());

                log.info("[SerpApi] Comercios recibidos: {}",
                        places.size());

                received += places.size();

                for (JsonNode place : places) {

                    if (businesses.size() >= MAX_RESULTS) {
                        break;
                    }

                    String name = place.path("title")
                            .asText("")
                            .trim();

                    String placeId = firstText(
                            place,
                            "place_id",
                            "data_id",
                            "data_cid"
                    );

                    if (name.isBlank()) {
                        missingData++;
                        continue;
                    }

                    if (placeId.isBlank()) {
                        placeId = "SERPAPI_"
                                + normalize(name)
                                + "_"
                                + normalize(cleanCity);
                    }

                    if (!seen.add(placeId)) {
                        duplicates++;
                        continue;
                    }

                    Double rating = readDouble(
                            place.path("rating"));

                    Integer reviewCount = readInteger(
                            place.path("reviews"));

                    double finalRating =
                            rating != null ? rating : 0.0;

                    int finalReviewCount =
                            reviewCount != null ? reviewCount : 0;

                    if (rating != null
                            && (rating < 0 || rating > 5)) {
                        rejectedRating++;
                        continue;
                    }

                    if (rating != null
                            && (rating < MIN_RATING
                            || rating >= MAX_RATING)) {
                        rejectedRating++;
                        continue;
                    }

                    if (reviewCount != null
                            && reviewCount < 0) {
                        missingData++;
                        continue;
                    }

                    if (reviewCount != null
                            && reviewCount <= MIN_REVIEWS) {
                        rejectedReviews++;
                        continue;
                    }

                    Business business = Business.builder()
                            .source("SERPAPI_GOOGLE_MAPS")
                            .sourceBusinessId(placeId)
                            .name(name)
                            .city(cleanCity)
                            .category(cleanCategory)
                            .address(place.path("address").asText(""))
                            .phone(place.path("phone").asText(""))
                            .website(place.path("website").asText(""))
                            .rating(finalRating)
                            .reviewCount(finalReviewCount)
                            .build();

                    businesses.add(business);

                    log.debug(
                            "[SerpApi] Comercio válido: {} | Calificación: {} | Reseñas: {}",
                            name,
                            finalRating,
                            finalReviewCount
                    );
                }
            }

            log.info("[SerpApi] RESUMEN DE BÚSQUEDA");
            log.info("[SerpApi] Recibidos: {}", received);
            log.info("[SerpApi] Sin datos necesarios: {}", missingData);
            log.info("[SerpApi] Descartados por calificación: {}",
                    rejectedRating);
            log.info("[SerpApi] Descartados por reseñas: {}",
                    rejectedReviews);
            log.info("[SerpApi] Duplicados: {}", duplicates);
            log.info("[SerpApi] Comercios encontrados: {}",
                    businesses.size());

            return businesses;

        } catch (HttpStatusCodeException e) {

            log.error("[SerpApi] Error HTTP: {}",
                    e.getStatusCode());

            throw new IllegalStateException(
                    "SerpApi devolvió un error HTTP: "
                            + e.getStatusCode().value()
                            + ". Verificá el servicio y la configuración."
            );

        } catch (ResourceAccessException e) {

            log.error("[SerpApi] Error de conexión o tiempo de espera.");

            throw new IllegalStateException(
                    "No se pudo conectar con SerpApi o se agotó el tiempo de espera."
            );

        } catch (IllegalArgumentException | IllegalStateException e) {

            throw e;

        } catch (RestClientException e) {

            log.error("[SerpApi] Error de comunicación con el servicio.");

            throw new IllegalStateException(
                    "No se pudo completar la comunicación con SerpApi."
            );

        } catch (Exception e) {

            log.error(
                    "[SerpApi] Error interno. Tipo: {}",
                    e.getClass().getSimpleName()
            );

            throw new IllegalStateException(
                    "Ocurrió un error al procesar la búsqueda de comercios."
            );
        }
    }

    private String firstText(JsonNode node, String... fields) {

        for (String field : fields) {
            String value = node.path(field)
                    .asText("")
                    .trim();

            if (!value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    private Double readDouble(JsonNode node) {

        if (node == null
                || node.isMissingNode()
                || node.isNull()
                || node.isContainerNode()) {
            return null;
        }

        if (node.isNumber()) {
            return node.asDouble();
        }

        try {
            String value = node.asText("")
                    .trim()
                    .replace(",", ".");

            return value.isBlank()
                    ? null
                    : Double.parseDouble(value);

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer readInteger(JsonNode node) {

        if (node == null
                || node.isMissingNode()
                || node.isNull()
                || node.isContainerNode()) {
            return null;
        }

        if (node.isIntegralNumber()
                && node.canConvertToInt()) {
            return node.asInt();
        }

        try {
            String value = node.asText("").trim();

            if (value.isBlank()) {
                return null;
            }

            return Integer.parseInt(value);

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalize(String value) {

        return value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}