package org.example.web.stock.stockDetail.controller;

import org.example.web.stock.stockDetail.domain.ParameterDefaultUpdateForm;
import org.example.web.stock.stockDetail.service.StockDetailService;
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

@ExtendWith(MockitoExtension.class)
class StockDetailRestControllerTest {

    @Mock
    StockDetailService stockDetailService;

    StockDetailRestController controller;

    @Test
    void updateParameterDefaults_成功したとき200を返すこと() {
        controller = new StockDetailRestController(stockDetailService);
        ParameterDefaultUpdateForm form = new ParameterDefaultUpdateForm(
                List.of(new ParameterDefaultUpdateForm.ParameterDefaultItem(1, BigDecimal.TEN)));

        ResponseEntity<String> response = controller.updateParameterDefaults("7203", form);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("初期値を更新しました");
        verify(stockDetailService).updateCompanyParameterDefaults("7203", form);
    }

    @Test
    void updateParameterDefaults_例外発生時に500を返すこと() {
        controller = new StockDetailRestController(stockDetailService);
        ParameterDefaultUpdateForm form = new ParameterDefaultUpdateForm(List.of());
        doThrow(new RuntimeException("db error")).when(stockDetailService)
                .updateCompanyParameterDefaults("9999", form);

        ResponseEntity<String> response = controller.updateParameterDefaults("9999", form);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("初期値の更新に失敗しました");
    }
}
