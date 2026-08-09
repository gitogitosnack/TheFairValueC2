package org.example.web.stock.country.controller;

import org.example.web.stock.country.domain.CountryForm;
import org.example.web.stock.country.domain.CountryResponseDto;
import org.example.web.stock.country.service.CountryService;
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
class CountryRestControllerTest {

    @Mock
    CountryService countryService;

    CountryRestController controller;

    @Test
    void export_サービスの一覧からxlsxのレスポンスを生成すること() {
        controller = new CountryRestController(countryService);
        when(countryService.initialDispAll()).thenReturn(List.of(new CountryResponseDto(1, "JP", "日本")));

        ResponseEntity<byte[]> response = controller.export();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("countries.xlsx");
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void insert_サービスのinsertCountryInfoを呼び出すこと() {
        controller = new CountryRestController(countryService);
        CountryForm form = new CountryForm(null, "US", "アメリカ");

        controller.insert(form);

        verify(countryService).insertCountryInfo(form);
    }

    @Test
    void update_成功したとき200を返すこと() {
        controller = new CountryRestController(countryService);
        CountryForm form = new CountryForm(1, "JP", "日本");

        ResponseEntity<String> response = controller.update(form);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("更新に成功しました");
    }

    @Test
    void update_例外発生時に500を返すこと() {
        controller = new CountryRestController(countryService);
        CountryForm form = new CountryForm(999, "XX", "存在しない国");
        doThrow(new RuntimeException("not found")).when(countryService).updateCountryInfo(form);

        ResponseEntity<String> response = controller.update(form);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("更新失敗");
    }

    @Test
    void delete_成功したとき200を返すこと() {
        controller = new CountryRestController(countryService);

        ResponseEntity<String> response = controller.delete(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Deleted");
    }

    @Test
    void delete_例外発生時に500を返すこと() {
        controller = new CountryRestController(countryService);
        doThrow(new RuntimeException("not found")).when(countryService).deleteCountryInfoById(999);

        ResponseEntity<String> response = controller.delete(999);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("削除に失敗しました");
    }
}
