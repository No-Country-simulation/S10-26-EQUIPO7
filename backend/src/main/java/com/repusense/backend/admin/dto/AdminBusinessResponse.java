package com.repusense.backend.admin.dto;

public record AdminBusinessResponse(
        Long id,
        String name,
        String city,
        String category,
        String address,
        Double rating,
        Integer reviewCount,
        String leadStatus) {
}
