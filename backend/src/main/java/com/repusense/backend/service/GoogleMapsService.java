
package com.repusense.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repusense.backend.model.Business;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GoogleMapsService {

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

        // Consultas específicas para intentar encontrar
        // comercios con problemas de reputación.
        List<String> queries = List.of(
                cleanCategory + " con malas reseñas en "
                        + cleanCity + ", Argentina",
                cleanCategory + " baja calificación en "
                        + cleanCity + ", Argentina"
        );

        try {
            for (String query : queries) {

                if (businesses.size() >= MAX_RESULTS) {
                    break;
                }

                String url = UriComponentsBuilder
                        .fromHttpUrl(API_URL)
                        .queryParam("engine", "google_maps")
                        .queryParam("type", "search")
                        .queryParam("q", query)
                        .queryParam("hl", "es")
                        .queryParam("gl", "ar")
                        .queryParam("api_key", apiKey)
                        .build()
                        .encode()
                        .toUriString();

                System.out.println(
                        "[SerpApi] Consultando: " + query);

                var response = restTemplate.getForEntity(
                        url, String.class);

                JsonNode root = objectMapper.readTree(
                        response.getBody());

                System.out.println(
                        "[SerpApi] HTTP: "
                                + response.getStatusCode());

                System.out.println(
                        "[SerpApi] Campos recibidos: "
                                + fieldNames(root));

                if (root.has("error")) {
                    throw new IllegalStateException(
                            "SerpApi: "
                                    + root.path("error").asText());
                }

                JsonNode places = root.path("local_results");

                if (!places.isArray()) {
                    throw new IllegalStateException(
                            "SerpApi no devolvió resultados "
                                    + "en local_results.");
                }

                System.out.println(
                        "[SerpApi] Comercios recibidos: "
                                + places.size());

                received += places.size();

                for (JsonNode place : places) {

                    if (businesses.size() >= MAX_RESULTS) {
                        break;
                    }

                    String placeId =
                            place.path("place_id")
                                    .asText("").trim();

                    if (placeId.isBlank()) {
                        placeId = place.path("data_id")
                                .asText("").trim();
                    }

                    if (placeId.isBlank()) {
                        placeId = place.path("data_cid")
                                .asText("").trim();
                    }

                    String name =
                            place.path("title")
                                    .asText("").trim();

                    JsonNode ratingNode = place.path("rating");
                    JsonNode reviewsNode = place.path("reviews");

                    if (placeId.isBlank()
                            || name.isBlank()
                            || !ratingNode.isNumber()
                            || !reviewsNode.isIntegralNumber()
                            || !reviewsNode.canConvertToInt()) {

                        missingData++;
                        continue;
                    }

                    double rating = ratingNode.asDouble();
                    int reviewCount = reviewsNode.asInt();

                    System.out.println(
                            "[SerpApi] Comercio: " + name
                                    + " | Calificación: " + rating
                                    + " | Reseñas: " + reviewCount);

                    if (rating < 0 || rating > 5
                            || reviewCount < 0) {
                        missingData++;
                        continue;
                    }

                    if (rating < MIN_RATING
                            || rating >= MAX_RATING) {
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
                            .source("SERPAPI_GOOGLE_MAPS")
                            .sourceBusinessId(placeId)
                            .name(name)
                            .city(cleanCity)
                            .category(cleanCategory)
                            .address(place.path("address")
                                    .asText(""))
                            .phone(place.path("phone")
                                    .asText(""))
                            .website(place.path("website")
                                    .asText(""))
                            .rating(rating)
                            .reviewCount(reviewCount)
                            .build();

                    businesses.add(business);
                }
            }

            System.out.println("[SerpApi] RESUMEN");
            System.out.println(
                    "[SerpApi] Recibidos: " + received);
            System.out.println(
                    "[SerpApi] Sin datos necesarios: "
                            + missingData);
            System.out.println(
                    "[SerpApi] Descartados por calificación: "
                            + rejectedRating);
            System.out.println(
                    "[SerpApi] Descartados por reseñas: "
                            + rejectedReviews);
            System.out.println(
                    "[SerpApi] Duplicados: " + duplicates);
            System.out.println(
                    "[SerpApi] Leads potenciales encontrados: "
                            + businesses.size());

            return businesses;

        } catch (HttpStatusCodeException e) {

            throw new IllegalStateException(
                    "Error HTTP de SerpApi: "
                            + e.getStatusCode()
                            + " - "
                            + e.getResponseBodyAsString(), e);

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            throw e;

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Error al buscar comercios en SerpApi: "
                            + e.getMessage(), e);
        }
    }

    private String fieldNames(JsonNode node) {

        List<String> names = new ArrayList<>();

        node.fieldNames().forEachRemaining(names::add);

        return names.toString();
    }
}
