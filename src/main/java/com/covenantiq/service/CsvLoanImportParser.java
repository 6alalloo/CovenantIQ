package com.covenantiq.service;

import com.covenantiq.dto.request.LoanImportCsvRow;
import com.covenantiq.enums.LoanStatus;
import com.covenantiq.exception.PayloadTooLargeException;
import com.covenantiq.exception.UnsupportedFileTypeException;
import com.opencsv.CSVReaderHeaderAware;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CsvLoanImportParser {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final List<String> REQUIRED_HEADERS = List.of(
            "sourcesystem",
            "externalloanid",
            "borrowername",
            "principalamount",
            "startdate",
            "status"
    );

    public List<ParsedRow> parse(MultipartFile file) {
        validateFile(file);
        List<ParsedRow> rows = new ArrayList<>();
        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new InputStreamReader(file.getInputStream()))) {
            Map<String, String> line;
            int rowNumber = 1;
            while ((line = reader.readMap()) != null) {
                rowNumber++;
                Map<String, String> normalized = normalize(line);
                if (normalized.values().stream().allMatch(v -> v == null || v.isBlank())) {
                    continue;
                }
                validateHeaders(normalized);

                String sourceSystem = trimmed(normalized.get("sourcesystem"));
                String externalLoanId = trimmed(normalized.get("externalloanid"));
                String borrowerName = trimmed(normalized.get("borrowername"));
                try {
                    LoanImportCsvRow payload = new LoanImportCsvRow(
                            rowNumber,
                            required(normalized, "sourcesystem").trim(),
                            required(normalized, "externalloanid").trim(),
                            required(normalized, "borrowername").trim(),
                            parsePositiveDecimal(required(normalized, "principalamount"), "principalAmount"),
                            parseDate(required(normalized, "startdate"), "startDate"),
                            parseStatus(required(normalized, "status")),
                            parseOffsetDateTime(normalized.get("sourceupdatedat"))
                    );
                    rows.add(new ParsedRow(rowNumber, sourceSystem, externalLoanId, borrowerName, payload, null));
                } catch (IllegalArgumentException ex) {
                    rows.add(new ParsedRow(rowNumber, sourceSystem, externalLoanId, borrowerName, null, ex.getMessage()));
                }
            }
        } catch (UnsupportedFileTypeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnsupportedFileTypeException("Unable to read import CSV");
        }
        return rows;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UnsupportedFileTypeException("CSV file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new PayloadTooLargeException("File size exceeds 5MB limit");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new UnsupportedFileTypeException("Only .csv loan import files are supported");
        }
    }

    private Map<String, String> normalize(Map<String, String> line) {
        Map<String, String> normalized = new HashMap<>();
        for (Map.Entry<String, String> entry : line.entrySet()) {
            normalized.put(entry.getKey().trim().toLowerCase(Locale.ROOT), entry.getValue());
        }
        return normalized;
    }

    private void validateHeaders(Map<String, String> normalized) {
        for (String header : REQUIRED_HEADERS) {
            if (!normalized.containsKey(header)) {
                throw new UnsupportedFileTypeException("Missing required column: " + header);
            }
        }
    }

    private String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required value for " + key);
        }
        return value;
    }

    private String trimmed(String value) {
        return value == null ? null : value.trim();
    }

    private BigDecimal parsePositiveDecimal(String value, String field) {
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(field + " must be greater than zero");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid decimal value for " + field);
        }
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date value for " + field + "; expected yyyy-MM-dd");
        }
    }

    private OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid sourceUpdatedAt; expected ISO-8601 timestamp");
        }
    }

    private LoanStatus parseStatus(String value) {
        try {
            return LoanStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid status; supported values are ACTIVE and CLOSED");
        }
    }

    public record ParsedRow(
            int rowNumber,
            String sourceSystem,
            String externalLoanId,
            String borrowerName,
            LoanImportCsvRow payload,
            String validationMessage
    ) {
    }
}
