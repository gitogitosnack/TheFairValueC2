package org.example.web.common.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * マスタ一覧・銘柄一覧の「Excel出力」ボタンから使う共通のxlsx生成処理。
 * 各機能のRestControllerはヘッダー名と行データ（DTOから組み立てたList&lt;Object&gt;）を渡すだけでよい。
 */
public final class ExcelExportUtil {

    private ExcelExportUtil() {
    }

    public static ResponseEntity<byte[]> export(
            String sheetName
            ,List<String> headers
            ,List<List<Object>> rows
            ,String fileName
    ) {
        byte[] body = toXlsxBytes(sheetName, headers, rows);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        responseHeaders.setContentDisposition(
                ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build());

        return new ResponseEntity<>(body, responseHeaders, HttpStatus.OK);
    }

    private static byte[] toXlsxBytes(String sheetName, List<String> headers, List<List<Object>> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);

            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < headers.size(); col++) {
                headerRow.createCell(col).setCellValue(headers.get(col));
            }

            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<Object> values = rows.get(r);
                for (int col = 0; col < values.size(); col++) {
                    setCellValue(row.createCell(col), values.get(col));
                }
            }

            for (int col = 0; col < headers.size(); col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Excel出力に失敗しました", e);
        }
    }

    private static void setCellValue(Cell cell, Object value) {
        switch (value) {
            case null -> cell.setBlank();
            case Number number -> cell.setCellValue(number.doubleValue());
            default -> cell.setCellValue(value.toString());
        }
    }
}
