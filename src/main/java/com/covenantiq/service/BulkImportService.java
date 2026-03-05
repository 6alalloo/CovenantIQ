package com.covenantiq.service;

import com.covenantiq.dto.request.SubmitFinancialStatementRequest;
import com.covenantiq.dto.response.BulkImportSummaryResponse;
import com.covenantiq.dto.response.RowImportResultResponse;
import com.covenantiq.enums.PeriodType;
import com.covenantiq.exception.PayloadTooLargeException;
import com.covenantiq.exception.UnsupportedFileTypeException;
import com.opencsv.CSVReaderHeaderAware;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class BulkImportService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final LoanService loanService;
    private final FinancialStatementService financialStatementService;
    private final OutboxEventPublisher outboxEventPublisher;

    public BulkImportService(
            LoanService loanService,
            FinancialStatementService financialStatementService,
            OutboxEventPublisher outboxEventPublisher
    ) {
        this.loanService = loanService;
        this.financialStatementService = financialStatementService;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional
    public BulkImportSummaryResponse importStatements(Long loanId, MultipartFile file) {
        loanService.getLoan(loanId);
        validateFile(file);

        List<ImportRow> rows;
        try (InputStream inputStream = file.getInputStream()) {
            if (isCsv(file.getOriginalFilename())) {
                rows = parseCsv(inputStream);
            } else {
                rows = parseExcel(inputStream);
            }
        } catch (IOException ex) {
            throw new UnsupportedFileTypeException("Unable to read import file");
        }

        List<RowImportResultResponse> results = new ArrayList<>();
        Set<LocalDate> seenPeriodEndDates = new HashSet<>();
        int successCount = 0;
        int failureCount = 0;

        for (ImportRow row : rows) {
            try {
                if (!seenPeriodEndDates.add(row.periodEndDate)) {
                    throw new IllegalArgumentException("Duplicate periodEndDate in import file");
                }
                SubmitFinancialStatementRequest request = toRequest(row);
                financialStatementService.submitStatement(loanId, request);
                successCount++;
                results.add(new RowImportResultResponse(row.rowNumber, true, null));
            } catch (Exception ex) {
                failureCount++;
                results.add(new RowImportResultResponse(row.rowNumber, false, ex.getMessage()));
            }
        }

        BulkImportSummaryResponse summary = new BulkImportSummaryResponse(rows.size(), successCount, failureCount, results);
        outboxEventPublisher.publish("FinancialStatement", loanId, "FinancialStatementBulkImported", java.util.Map.of(
                "loanId", loanId,
                "totalRows", rows.size(),
                "successCount", successCount,
                "failureCount", failureCount
        ));
        return summary;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UnsupportedFileTypeException("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new PayloadTooLargeException("File size exceeds 5MB limit");
        }
        String filename = file.getOriginalFilename();
        if (!isCsv(filename) && !isXlsx(filename)) {
            throw new UnsupportedFileTypeException("Only .csv and .xlsx files are supported");
        }
    }

    private boolean isCsv(String filename) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    private boolean isXlsx(String filename) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".xlsx");
    }

    private List<ImportRow> parseCsv(InputStream inputStream) throws IOException {
        List<ImportRow> rows = new ArrayList<>();
        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new InputStreamReader(inputStream))) {
            Map<String, String> line;
            int rowNumber = 1;
            while ((line = reader.readMap()) != null) {
                rowNumber++;
                if (line.values().stream().allMatch(v -> v == null || v.isBlank())) {
                    continue;
                }
                rows.add(toImportRow(rowNumber, normalize(line)));
            }
        } catch (Exception ex) {
            throw new IOException("Malformed CSV file", ex);
        }
        return rows;
    }

    private List<ImportRow> parseExcel(InputStream inputStream) throws IOException {
        List<ImportRow> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return rows;
            }
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                headers.add(formatter.formatCellValue(headerRow.getCell(i)).trim().toLowerCase(Locale.ROOT));
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new java.util.HashMap<>();
                boolean empty = true;
                for (int c = 0; c < headers.size(); c++) {
                    String value = formatter.formatCellValue(row.getCell(c));
                    if (value != null && !value.isBlank()) {
                        empty = false;
                    }
                    values.put(headers.get(c), value);
                }
                if (empty) {
                    continue;
                }
                rows.add(toImportRow(i + 1, values));
            }
        }
        return rows;
    }

    private Map<String, String> normalize(Map<String, String> line) {
        Map<String, String> normalized = new java.util.HashMap<>();
        for (Map.Entry<String, String> entry : line.entrySet()) {
            normalized.put(entry.getKey().trim().toLowerCase(Locale.ROOT), entry.getValue());
        }
        return normalized;
    }

    private ImportRow toImportRow(int rowNumber, Map<String, String> values) {
        LocalDate periodEndDate = LocalDate.parse(required(values, "periodenddate"));
        return new ImportRow(
                rowNumber,
                periodEndDate,
                bigDecimal(required(values, "currentassets")),
                bigDecimal(required(values, "currentliabilities")),
                bigDecimal(required(values, "totaldebt")),
                bigDecimal(required(values, "totalequity")),
                bigDecimal(required(values, "ebit")),
                bigDecimal(required(values, "interestexpense")),
                nullableBigDecimal(values.get("netoperatingincome")),
                nullableBigDecimal(values.get("totaldebtservice")),
                nullableBigDecimal(values.get("intangibleassets")),
                nullableBigDecimal(values.get("ebitda")),
                nullableBigDecimal(values.get("fixedcharges")),
                nullableBigDecimal(values.get("inventory")),
                nullableBigDecimal(values.get("totalassets")),
                nullableBigDecimal(values.get("totalliabilities"))
        );
    }

    private SubmitFinancialStatementRequest toRequest(ImportRow row) {
        PeriodType periodType = inferPeriodType(row.periodEndDate);
        Integer fiscalQuarter = periodType == PeriodType.ANNUAL ? null : inferQuarter(row.periodEndDate);
        return new SubmitFinancialStatementRequest(
                periodType,
                row.periodEndDate.getYear(),
                fiscalQuarter,
                nonNegative(row.currentAssets, "currentAssets"),
                nonNegative(row.currentLiabilities, "currentLiabilities"),
                nonNegative(row.totalDebt, "totalDebt"),
                nonNegative(row.totalEquity, "totalEquity"),
                nonNegative(row.ebit, "ebit"),
                nonNegative(row.interestExpense, "interestExpense"),
                nonNegativeNullable(row.netOperatingIncome, "netOperatingIncome"),
                nonNegativeNullable(row.totalDebtService, "totalDebtService"),
                nonNegativeNullable(row.intangibleAssets, "intangibleAssets"),
                nonNegativeNullable(row.ebitda, "ebitda"),
                nonNegativeNullable(row.fixedCharges, "fixedCharges"),
                nonNegativeNullable(row.inventory, "inventory"),
                nonNegativeNullable(row.totalAssets, "totalAssets"),
                nonNegativeNullable(row.totalLiabilities, "totalLiabilities"),
                OffsetDateTime.of(row.periodEndDate.atTime(12, 0), ZoneOffset.UTC)
        );
    }

    private PeriodType inferPeriodType(LocalDate date) {
        if (date.getMonthValue() == 12 && date.getDayOfMonth() == 31) {
            return PeriodType.ANNUAL;
        }
        return PeriodType.QUARTERLY;
    }

    private Integer inferQuarter(LocalDate date) {
        return ((date.getMonthValue() - 1) / 3) + 1;
    }

    private BigDecimal bigDecimal(String value) {
        return new BigDecimal(value.trim());
    }

    private BigDecimal nullableBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value.trim());
    }

    private String required(Map<String, String> map, String key) {
        String value = map.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required column: " + key);
        }
        return value;
    }

    private BigDecimal nonNegative(BigDecimal value, String field) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
        return value;
    }

    private BigDecimal nonNegativeNullable(BigDecimal value, String field) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
        return value;
    }

    private record ImportRow(
            int rowNumber,
            LocalDate periodEndDate,
            BigDecimal currentAssets,
            BigDecimal currentLiabilities,
            BigDecimal totalDebt,
            BigDecimal totalEquity,
            BigDecimal ebit,
            BigDecimal interestExpense,
            BigDecimal netOperatingIncome,
            BigDecimal totalDebtService,
            BigDecimal intangibleAssets,
            BigDecimal ebitda,
            BigDecimal fixedCharges,
            BigDecimal inventory,
            BigDecimal totalAssets,
            BigDecimal totalLiabilities
    ) {
    }
}
