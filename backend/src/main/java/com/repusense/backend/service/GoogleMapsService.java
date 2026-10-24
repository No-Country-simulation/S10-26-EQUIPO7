package com.repusense.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.repusense.backend.model.Business;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleMapsService {

    @Value("${google.maps.api.key:DEFAULT_KEY}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<Business> fetchBusinessesByCityAndCategory(String city, String category) {
        List<Business> results = new ArrayList<>();
        String query = category + " en " + city;
        String baseUrl = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=" 
                        + query.replace(" ", "%20") + "&key=" + apiKey;

        String url = baseUrl;
        int maxPages = 3; // Límite de páginas por búsqueda

        while (url != null && maxPages > 0) {
            try {
                JsonNode response = restTemplate.getForObject(url, JsonNode.class);
                if (response == null || !response.has("results")) break;

                for (JsonNode place : response.get("results")) {
                    String placeId = place.has("place_id") ? place.get("place_id").asText() : null;

                    Business b = Business.builder()
                            .source("GOOGLE_MAPS")
                            .sourceBusinessId(placeId != null ? placeId : "MAPS_" + System.currentTimeMillis())
                            .name(place.has("name") ? place.get("name").asText() : "Sin nombre")
                            .city(city)
                            .category(category)
                            .rating(place.has("rating") ? place.get("rating").asDouble() : 0.0)
                            .reviewCount(place.has("user_ratings_total") ? place.get("user_ratings_total").asInt() : 0)
                            .address(place.has("formatted_address") ? place.get("formatted_address").asText() : "")
                            .build();
                    results.add(b);
                }

                if (response.has("next_page_token")) {
                    String pageToken = response.get("next_page_token").asText();
                    Thread.sleep(2000); // Pausa requerida por la API de Google
                    url = baseUrl + "&pagetoken=" + pageToken;
                } else {
                    url = null;
                }
            } catch (Exception e) {
                break;
            }
            maxPages--;
        }
        return results;
    }
}