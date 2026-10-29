
package com.repusense.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repusense.backend.model.Business;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GoogleMapsService {

    private static final String API_URL =
            "https://api.fetchlayer.dev/google-maps/place-search";

    private static final int MAX_RESULTS = 200;
    private static final int MAX_PAGES = 20;
    private static final int PAGE_SIZE = 10;
    private static final double MIN_RATING = 1.5;
    private static final double MAX_RATING = 3.5;
    private static final int MIN_REVIEWS = 10;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public GoogleMapsService(
            @Value("${fetchlayer.api.key:}") String apiKey) {

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
            throw new IllegalArgumentException("Ingresá una ciudad.");
        }

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Ingresá un rubro.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar FETCHLAYER_API_KEY.");
        }

        String cleanCity = city.trim();
        String cleanCategory = category.trim();

        List<Business> businesses = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String cursor = null;

        int received = 0;
        int missingData = 0;
        int rejectedRating = 0;
        int rejectedReviews = 0;
        int duplicates = 0;

        try {
            for (int page = 0;
                 page < MAX_PAGES && businesses.size() < MAX_RESULTS;
                 page++) {

                var body = objectMapper.createObjectNode();
                body.put("query", cleanCategory);
                body.put("location", cleanCity + ", Argentina");
                body.put("language", "es");
                body.put("country", "AR");
                body.put("limit", PAGE_SIZE);

                if (cursor != null && !cursor.isBlank()) {
                    body.put("cursor", cursor);
                }

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(apiKey);
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(List.of(MediaType.APPLICATION_JSON));

                HttpEntity<String> request = new HttpEntity<>(
                        objectMapper.writeValueAsString(body),
                        headers
                );

                ResponseEntity<String> response = restTemplate.exchange(
                        API_URL,
                        HttpMethod.POST,
                        request,
                        String.class
                );

                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode places = root.path("places");

                System.out.println("[FetchLayer] Página: " + (page + 1));
                System.out.println("[FetchLayer] HTTP: "
                        + response.getStatusCode());
                System.out.println("[FetchLayer] Campos recibidos: "
                        + fieldNames(root));

                if (!places.isArray()) {
                    System.out.println("[FetchLayer] Respuesta recibida: "
                            + root);

                    throw new IllegalStateException(
                            "FetchLayer no devolvió una lista en el campo "
                                    + "'places'.");
                }

                System.out.println("[FetchLayer] Comercios recibidos: "
                        + places.size());

                received += places.size();

                for (JsonNode place : places) {
                    if (businesses.size() >= MAX_RESULTS) {
                        break;
                    }

                    String placeId =
                            place.path("placeId").asText("").trim();

                    String name =
                            place.path("name").asText("").trim();

                    JsonNode ratingNode = place.path("rating");
                    JsonNode reviewsNode = place.path("reviewCount");

                    if (placeId.isBlank()
                            || name.isBlank()
                            || !ratingNode.isNumber()
                            || !reviewsNode.isIntegralNumber()
                            || !reviewsNode.canConvertToInt()) {

                        missingData++;

                        System.out.println(
                                "[FetchLayer] Comercio descartado: "
                                        + name
                                        + " | Faltan identificador, nombre, "
                                        + "calificación o cantidad de reseñas."
                        );

                        continue;
                    }

                    double rating = ratingNode.asDouble();
                    int reviewCount = reviewsNode.asInt();

                    System.out.println(
                            "[FetchLayer] Comercio: " + name
                                    + " | Calificación: " + rating
                                    + " | Reseñas: " + reviewCount
                    );

                    if (rating < 0 || rating > 5 || reviewCount < 0) {
                        missingData++;
                        continue;
                    }

                    if (rating < MIN_RATING || rating >= MAX_RATING) {
                        rejectedRating++;
                        continue;
                    }

                    if (reviewCount <= MIN_REVIEWS) {
                        rejectedReviews++;
                        continue;
                    }

                    if (!seen.add(placeId)) {
                        duplicates++;
                        continue;
                    }

                    Business business = Business.builder()
                            .source("FETCHLAYER_GOOGLE_MAPS")
                            .sourceBusinessId(placeId)
                            .name(name)
                            .city(cleanCity)
                            .category(cleanCategory)
                            .address(place.path("address").asText(""))
                            .phone(place.path("phone").asText(""))
                            .website(place.path("website").asText(""))
                            .rating(rating)
                            .reviewCount(reviewCount)
                            .build();

                    businesses.add(business);
                }

                cursor = root.path("nextCursor").asText("");

                if (cursor.isBlank()) {
                    System.out.println(
                            "[FetchLayer] No hay más páginas disponibles.");
                    break;
                }
            }

            System.out.println("[FetchLayer] RESUMEN");
            System.out.println("[FetchLayer] Recibidos: " + received);
            System.out.println("[FetchLayer] Sin datos necesarios: "
                    + missingData);
            System.out.println("[FetchLayer] Descartados por calificación: "
                    + rejectedRating);
            System.out.println("[FetchLayer] Descartados por reseñas: "
                    + rejectedReviews);
            System.out.println("[FetchLayer] Duplicados: " + duplicates);
            System.out.println("[FetchLayer] Leads potenciales encontrados: "
                    + businesses.size());

            return businesses;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error al buscar comercios en FetchLayer: "
                            + e.getMessage(), e);
        }
    }

    private String fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names.toString();
    }
}
