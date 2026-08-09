package org.example.web.common.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelExportUtilTest {

    @Test
    void export_ヘッダー行とデータ行を含むxlsxを生成すること() throws IOException {
        List<String> headers = List.of("ID", "コード", "名前");
        List<List<Object>> rows = List.of(
                List.of(1, "JP", "日本"),
                List.of(2, "US", "アメリカ"));

        ResponseEntity<byte[]> response = ExcelExportUtil.export("国マスタ", headers, rows, "countries.xlsx");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("countries.xlsx");
        assertThat(response.getHeaders().getContentType())
                .hasToString("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            Sheet sheet = workbook.getSheet("国マスタ");
            assertThat(sheet).isNotNull();

            Row headerRow = sheet.getRow(0);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("ID");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("コード");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("名前");

            Row dataRow1 = sheet.getRow(1);
            assertThat(dataRow1.getCell(0).getNumericCellValue()).isEqualTo(1.0);
            assertThat(dataRow1.getCell(1).getStringCellValue()).isEqualTo("JP");
            assertThat(dataRow1.getCell(2).getStringCellValue()).isEqualTo("日本");

            Row dataRow2 = sheet.getRow(2);
            assertThat(dataRow2.getCell(0).getNumericCellValue()).isEqualTo(2.0);
            assertThat(dataRow2.getCell(1).getStringCellValue()).isEqualTo("US");
        }
    }

    @Test
    void export_null値を含むセルは空白セルになること() throws IOException {
        List<String> headers = List.of("ID", "説明");
        List<List<Object>> rows = List.of(Arrays.asList(1, null));

        ResponseEntity<byte[]> response = ExcelExportUtil.export("テスト", headers, rows, "test.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            Row dataRow = workbook.getSheet("テスト").getRow(1);
            assertThat(dataRow.getCell(1).getCellType().toString()).isEqualTo("BLANK");
        }
    }

    @Test
    void export_行が0件でもヘッダーだけのxlsxを生成すること() throws IOException {
        List<String> headers = List.of("ID", "名前");
        List<List<Object>> rows = List.of();

        ResponseEntity<byte[]> response = ExcelExportUtil.export("空データ", headers, rows, "empty.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            Sheet sheet = workbook.getSheet("空データ");
            assertThat(sheet.getRow(0)).isNotNull();
            assertThat(sheet.getRow(1)).isNull();
        }
    }
}
