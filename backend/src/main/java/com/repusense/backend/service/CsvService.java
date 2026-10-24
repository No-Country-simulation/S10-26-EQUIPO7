package com.repusense.backend.service;

import com.repusense.backend.model.Business;
import org.apache.commons.csv.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvService {

    public ByteArrayInputStream exportBusinessesToCsv(List<Business> businesses) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("ID", "Source", "SourceBusinessId", "Nombre", "Ciudad", "Categoria", "Rating", "TotalResenas", "Telefono", "Website")
                .build();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out, true, StandardCharsets.UTF_8), format)) {

            for (Business b : businesses) {
                csvPrinter.printRecord(
                        b.getId(),
                        b.getSource(),
                        b.getSourceBusinessId(),
                        b.getName(),
                        b.getCity(),
                        b.getCategory(),
                        b.getRating(),
                        b.getReviewCount(),
                        b.getPhone(),
                        b.getWebsite()
                );
            }
            csvPrinter.flush();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error al exportar datos a CSV: " + e.getMessage());
        }
    }

    public List<Business> parseCsvToBusinesses(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).build())) {

            List<Business> list = new ArrayList<>();
            for (CSVRecord record : csvParser) {
                Business b = Business.builder()
                        .source(record.isMapped("Source") ? record.get("Source") : "CSV_IMPORT")
                        .sourceBusinessId(record.isMapped("SourceBusinessId") ? record.get("SourceBusinessId") : "CSV_" + System.currentTimeMillis())
                        .name(record.get("Nombre"))
                        .city(record.isMapped("Ciudad") ? record.get("Ciudad") : "Desconocida")
                        .category(record.isMapped("Categoria") ? record.get("Categoria") : "General")
                        .rating(record.isMapped("Rating") && !record.get("Rating").isEmpty() ? Double.parseDouble(record.get("Rating")) : 0.0)
                        .reviewCount(record.isMapped("TotalResenas") && !record.get("TotalResenas").isEmpty() ? Integer.parseInt(record.get("TotalResenas")) : 0)
                        .phone(record.isMapped("Telefono") ? record.get("Telefono") : null)
                        .website(record.isMapped("Website") ? record.get("Website") : null)
                        .build();
                list.add(b);
            }
            return list;
        } catch (IOException e) {
            throw new RuntimeException("Error al procesar el archivo CSV: " + e.getMessage());
        }
    }
}