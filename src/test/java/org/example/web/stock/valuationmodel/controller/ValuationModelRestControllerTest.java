package org.example.web.stock.valuationmodel.controller;

import org.example.web.stock.valuationmodel.domain.ValuationModelForm;
import org.example.web.stock.valuationmodel.domain.ValuationModelResponseDto;
import org.example.web.stock.valuationmodel.service.ValuationModelService;
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
class ValuationModelRestControllerTest {

    @Mock
    ValuationModelService valuationModelService;

    ValuationModelRestController controller;

    @Test
    void export_サービスの一覧からxlsxのレスポンスを生成すること() {
        controller = new ValuationModelRestController(valuationModelService);
        when(valuationModelService.initialDispAll())
                .thenReturn(List.of(new ValuationModelResponseDto(1, "DCF法", "説明")));

        ResponseEntity<byte[]> response = controller.export();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("valuation_models.xlsx");
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void insert_サービスのinsertValuationModelInfoを呼び出すこと() {
        controller = new ValuationModelRestController(valuationModelService);
        ValuationModelForm form = new ValuationModelForm(null, "PERマルチプル法", "説明");

        controller.insert(form);

        verify(valuationModelService).insertValuationModelInfo(form);
    }

    @Test
    void update_成功したとき200を返すこと() {
        controller = new ValuationModelRestController(valuationModelService);
        ValuationModelForm form = new ValuationModelForm(1, "DCF法", "説明");

        ResponseEntity<String> response = controller.update(form);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("更新に成功しました");
    }

    @Test
    void update_例外発生時に500を返すこと() {
        controller = new ValuationModelRestController(valuationModelService);
        ValuationModelForm form = new ValuationModelForm(999, "存在しないモデル", null);
        doThrow(new RuntimeException("not found")).when(valuationModelService).updateValuationModelInfo(form);

        ResponseEntity<String> response = controller.update(form);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("更新失敗");
    }

    @Test
    void delete_成功したとき200を返すこと() {
        controller = new ValuationModelRestController(valuationModelService);

        ResponseEntity<String> response = controller.delete(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Deleted");
    }

    @Test
    void delete_例外発生時に500を返すこと() {
        controller = new ValuationModelRestController(valuationModelService);
        doThrow(new RuntimeException("not found")).when(valuationModelService).deleteValuationModelInfoById(999);

        ResponseEntity<String> response = controller.delete(999);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("削除に失敗しました");
    }
}
