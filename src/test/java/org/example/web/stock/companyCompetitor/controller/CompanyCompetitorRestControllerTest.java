package org.example.web.stock.companyCompetitor.controller;

import org.example.web.stock.companyCompetitor.domain.CompanyOptionDto;
import org.example.web.stock.companyCompetitor.domain.CompetitorForm;
import org.example.web.stock.companyCompetitor.domain.CompetitorResponseDto;
import org.example.web.stock.companyCompetitor.service.CompanyCompetitorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyCompetitorRestControllerTest {

    @Mock
    CompanyCompetitorService companyCompetitorService;

    CompanyCompetitorRestController controller;

    @Test
    void getByCompany_サービスの結果をそのまま返すこと() {
        controller = new CompanyCompetitorRestController(companyCompetitorService);
        List<CompetitorResponseDto> items = List.of(new CompetitorResponseDto(1, "7267", "ホンダ"));
        when(companyCompetitorService.getCompetitorsByCompanyCode("7203")).thenReturn(items);

        List<CompetitorResponseDto> result = controller.getByCompany("7203");

        assertThat(result).isEqualTo(items);
    }

    @Test
    void getAllCompanyOptions_サービスの結果をそのまま返すこと() {
        controller = new CompanyCompetitorRestController(companyCompetitorService);
        List<CompanyOptionDto> items = List.of(new CompanyOptionDto("7203", "トヨタ自動車"));
        when(companyCompetitorService.getAllCompanyOptions()).thenReturn(items);

        List<CompanyOptionDto> result = controller.getAllCompanyOptions();

        assertThat(result).isEqualTo(items);
    }

    @Test
    void insert_成功したとき200を返すこと() {
        controller = new CompanyCompetitorRestController(companyCompetitorService);
        CompetitorForm form = new CompetitorForm("7203", "7267");

        ResponseEntity<String> response = controller.insert(form);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(companyCompetitorService).insertCompetitor(form);
    }

    @Test
    void insert_不正な引数のとき400を返すこと() {
        controller = new CompanyCompetitorRestController(companyCompetitorService);
        CompetitorForm form = new CompetitorForm("7203", "7203");
        doThrow(new IllegalArgumentException("自社を競合として登録することはできません。"))
                .when(companyCompetitorService).insertCompetitor(form);

        ResponseEntity<String> response = controller.insert(form);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("自社を競合として登録することはできません。");
    }

    @Test
    void insert_その他の例外発生時に500を返すこと() {
        controller = new CompanyCompetitorRestController(companyCompetitorService);
        CompetitorForm form = new CompetitorForm("7203", "9999");
        doThrow(new RuntimeException("db error")).when(companyCompetitorService).insertCompetitor(form);

        ResponseEntity<String> response = controller.insert(form);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("競合の登録に失敗しました");
    }

    @Test
    void delete_成功したとき200を返すこと() {
        controller = new CompanyCompetitorRestController(companyCompetitorService);

        ResponseEntity<String> response = controller.delete(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Deleted");
    }

    @Test
    void delete_例外発生時に500を返すこと() {
        controller = new CompanyCompetitorRestController(companyCompetitorService);
        doThrow(new RuntimeException("not found")).when(companyCompetitorService).deleteCompetitorById(999);

        ResponseEntity<String> response = controller.delete(999);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("削除に失敗しました");
    }
}
