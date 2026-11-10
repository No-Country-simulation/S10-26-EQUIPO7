package com.repusense.backend.admin.dto;

import java.util.List;

public record DeleteBusinessesResponse(
        int deletedCount,
        List<Long> deletedIds,
        String message) {
}
