package org.example.web.stock.currency.controller;

import org.example.web.stock.currency.domain.CurrencyForm;
import org.example.web.stock.currency.domain.CurrencyResponseDto;
import org.example.web.stock.currency.service.CurrencyService;
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
class CurrencyRestControllerTest {

    @Mock
    CurrencyService currencyService;

    CurrencyRestController controller;

    @Test
    void export_サービスの一覧からxlsxのレスポンスを生成すること() {
        controller = new CurrencyRestController(currencyService);
        when(currencyService.initialDispAll()).thenReturn(List.of(new CurrencyResponseDto(1, "JPY", "¥")));

        ResponseEntity<byte[]> response = controller.export();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("currencies.xlsx");
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void insert_サービスのinsertCurrencyInfoを呼び出すこと() {
        controller = new CurrencyRestController(currencyService);
        CurrencyForm form = new CurrencyForm(null, "USD", "$");

        controller.insert(form);

        verify(currencyService).insertCurrencyInfo(form);
    }

    @Test
    void update_成功したとき200を返すこと() {
        controller = new CurrencyRestController(currencyService);
        CurrencyForm form = new CurrencyForm(1, "JPY", "¥");

        ResponseEntity<String> response = controller.update(form);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("更新に成功しました");
    }

    @Test
    void update_例外発生時に500を返すこと() {
        controller = new CurrencyRestController(currencyService);
        CurrencyForm form = new CurrencyForm(999, "XXX", "?");
        doThrow(new RuntimeException("not found")).when(currencyService).updateCurrencyInfo(form);

        ResponseEntity<String> response = controller.update(form);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("更新失敗");
    }

    @Test
    void delete_成功したとき200を返すこと() {
        controller = new CurrencyRestController(currencyService);

        ResponseEntity<String> response = controller.delete(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Deleted");
    }

    @Test
    void delete_例外発生時に500を返すこと() {
        controller = new CurrencyRestController(currencyService);
        doThrow(new RuntimeException("not found")).when(currencyService).deleteCurrencyInfoById(999);

        ResponseEntity<String> response = controller.delete(999);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("削除に失敗しました");
    }
}
