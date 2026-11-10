package com.repusense.backend.admin;

import com.repusense.backend.admin.dto.AdminBusinessResponse;
import com.repusense.backend.admin.dto.DeleteBusinessesRequest;
import com.repusense.backend.admin.dto.DeleteBusinessesResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/businesses")
@SecurityRequirement(name = "bearerAuth")
public class AdminBusinessController {

    private final AdminBusinessService adminBusinessService;

    public AdminBusinessController(AdminBusinessService adminBusinessService) {
        this.adminBusinessService = adminBusinessService;
    }

    @GetMapping
    public ResponseEntity<List<AdminBusinessResponse>> getBusinesses() {
        return ResponseEntity.ok(adminBusinessService.getBusinesses());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteBusinessesResponse> deleteBusiness(
            @PathVariable Long id) {
        return ResponseEntity.ok(adminBusinessService.deleteBusiness(id));
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<DeleteBusinessesResponse> deleteBusinesses(
            @Valid @RequestBody DeleteBusinessesRequest request) {
        return ResponseEntity.ok(adminBusinessService.deleteBusinesses(request.ids()));
    }
}
