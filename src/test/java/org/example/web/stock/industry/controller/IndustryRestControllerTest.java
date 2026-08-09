package org.example.web.stock.industry.controller;

import org.example.web.stock.industry.domain.IndustryForm;
import org.example.web.stock.industry.domain.IndustryResponseDto;
import org.example.web.stock.industry.service.IndustryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndustryRestControllerTest {

    @Mock
    IndustryService industryService;

    IndustryRestController controller;

    @Test
    void export_サービスの一覧からxlsxのレスポンスを生成すること() {
        controller = new IndustryRestController(industryService);
        when(industryService.initialDispAll())
                .thenReturn(List.of(new IndustryResponseDto(1, "自動車", null, null, null)));

        ResponseEntity<byte[]> response = controller.export();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("industries.xlsx");
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void insert_サービスのinsertIndustryInfoを呼び出すこと() {
        controller = new IndustryRestController(industryService);
        IndustryForm form = new IndustryForm(null, "小売業", "小売", "説明", BigDecimal.TEN);

        controller.insert(form);

        verify(industryService).insertIndustryInfo(form);
    }

    @Test
    void update_成功したとき200を返すこと() {
        controller = new IndustryRestController(industryService);
        IndustryForm form = new IndustryForm(1, "自動車", "輸送用機器", "説明", BigDecimal.TEN);

        ResponseEntity<String> response = controller.update(form);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("更新に成功しました");
    }

    @Test
    void update_例外発生時に500を返すこと() {
        controller = new IndustryRestController(industryService);
        IndustryForm form = new IndustryForm(999, "存在しない業種", null, null, null);
        doThrow(new RuntimeException("not found")).when(industryService).updateIndustryInfo(form);

        ResponseEntity<String> response = controller.update(form);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("更新失敗");
    }

    @Test
    void delete_成功したとき200を返すこと() {
        controller = new IndustryRestController(industryService);

        ResponseEntity<String> response = controller.delete(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Deleted");
    }

    @Test
    void delete_例外発生時に500を返すこと() {
        controller = new IndustryRestController(industryService);
        doThrow(new RuntimeException("not found")).when(industryService).deleteIndustryInfoById(999);

        ResponseEntity<String> response = controller.delete(999);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("削除に失敗しました");
    }
}
