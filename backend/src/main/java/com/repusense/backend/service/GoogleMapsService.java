
package com.repusense.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repusense.backend.model.Business;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class GoogleMapsService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoogleMapsService() {
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(35000);
        this.restTemplate = new RestTemplate(factory);
    }

    public List<Business> fetchBusinessesByCityAndCategory(
            String city, String category) {

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT,
                    "RepuSense/1.0 (desarrollo local)");

            HttpEntity<Void> request = new HttpEntity<>(headers);

            var geocodingUri = UriComponentsBuilder
                    .fromUriString(
                            "https://nominatim.openstreetmap.org/search")
                    .queryParam("q", city + ", Argentina")
                    .queryParam("countrycodes", "ar")
                    .queryParam("format", "jsonv2")
                    .queryParam("limit", 1)
                    .build()
                    .encode()
                    .toUri();

            ResponseEntity<String> geocodingResponse =
                    restTemplate.exchange(
                            geocodingUri,
                            HttpMethod.GET,
                            request,
                            String.class);

            JsonNode locations = objectMapper.readTree(
                    geocodingResponse.getBody());

            if (!locations.isArray() || locations.isEmpty()) {
                throw new IllegalStateException(
                        "No se encontró la ciudad: " + city);
            }

            double lat = locations.get(0).path("lat").asDouble();
            double lon = locations.get(0).path("lon").asDouble();

            String selectors = buildSelectors(category, lat, lon);
            String query = "[out:json][timeout:25];("
                    + selectors
                    + ");out center tags;";

            String[] servers = {
                    "https://overpass.kumi.systems/api/interpreter",
                    "https://overpass.private.coffee/api/interpreter",
                    "https://overpass-api.de/api/interpreter"
            };

            String responseBody = null;
            Exception lastError = null;

            for (String server : servers) {
                try {
                    var overpassUri = UriComponentsBuilder
                            .fromUriString(server)
                            .queryParam("data", query)
                            .build()
                            .encode()
                            .toUri();

                    ResponseEntity<String> response =
                            restTemplate.exchange(
                                    overpassUri,
                                    HttpMethod.GET,
                                    request,
                                    String.class);

                    responseBody = response.getBody();

                    if (responseBody != null && !responseBody.isBlank()) {
                        break;
                    }
                } catch (Exception e) {
                    lastError = e;
                }
            }

            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalStateException(
                        "No se pudo obtener respuesta de los servidores "
                                + "de OpenStreetMap. Último error: "
                                + (lastError != null
                                ? lastError.getMessage()
                                : "respuesta vacía"));
            }

            JsonNode elements = objectMapper.readTree(responseBody)
                    .path("elements");

            List<Business> businesses = new ArrayList<>();

            for (JsonNode element : elements) {
                JsonNode tags = element.path("tags");
                String name = tags.path("name").asText("").trim();

                if (name.isBlank()) {
                    continue;
                }

                String type = element.path("type").asText();
                long id = element.path("id").asLong();

                String street = tags.path("addr:street").asText("");
                String number = tags.path("addr:housenumber").asText("");
                String address = (street + " " + number).trim();

                if (address.isBlank()) {
                    address = tags.path("addr:full").asText(city);
                }

                String phone = tags.path("phone")
                        .asText(tags.path("contact:phone").asText(""));

                String website = tags.path("website")
                        .asText(tags.path("contact:website").asText(""));

                Business business = Business.builder()
                        .source("OPENSTREETMAP")
                        .sourceBusinessId("OSM_" + type + "_" + id)
                        .name(name)
                        .city(city)
                        .category(category)
                        .address(address)
                        .phone(phone)
                        .website(website)
                        .rating(0.0)
                        .reviewCount(0)
                        .build();

                businesses.add(business);
            }

            return businesses;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error al consultar OpenStreetMap: "
                            + e.getMessage(), e);
        }
    }

    private String buildSelectors(
            String category, double lat, double lon) {

        String normalized = Normalizer.normalize(
                        category == null ? "" : category,
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);

        String around = "(around:10000," + lat + "," + lon + ")";

        if (normalized.contains("farmacia")) {
            return "nwr" + around
                    + "[\"name\"][\"amenity\"=\"pharmacy\"];";
        }

        if (normalized.contains("dent") || normalized.contains("clinic")) {
            return "nwr" + around
                    + "[\"name\"][\"amenity\"~\"clinic|dentist|doctors\"];";
        }

        if (normalized.contains("universidad")) {
            return "nwr" + around
                    + "[\"name\"][\"amenity\"~\"university|college\"];";
        }

        if (normalized.contains("hotel")) {
            return "nwr" + around
                    + "[\"name\"][\"tourism\"~\"hotel|motel|hostel|guest_house\"];";
        }

        if (normalized.contains("gimnas") || normalized.contains("gym")) {
            return "nwr" + around
                    + "[\"name\"][\"leisure\"=\"fitness_centre\"];";
        }

        if (normalized.contains("panader")) {
            return "nwr" + around
                    + "[\"name\"][\"shop\"=\"bakery\"];";
        }

        if (normalized.contains("restaurante")
                || normalized.contains("restaurant")) {
            return "nwr" + around
                    + "[\"name\"][\"amenity\"~\"restaurant|fast_food|cafe\"];";
        }

        return "nwr" + around
                + "[\"name\"][\"amenity\"~\"pharmacy|clinic|dentist|university|college|restaurant|cafe|fast_food|bank|fuel|hospital|doctors\"];"
                + "nwr" + around
                + "[\"name\"][\"shop\"~\"bakery|supermarket|convenience\"];"
                + "nwr" + around
                + "[\"name\"][\"tourism\"~\"hotel|hostel|guest_house\"];"
                + "nwr" + around
                + "[\"name\"][\"leisure\"=\"fitness_centre\"];";
    }
}
