package org.example.web.stock.currency.controller;

import org.example.web.stock.currency.domain.CurrencyResponseDto;
import org.example.web.stock.currency.service.CurrencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyControllerTest {

    @Mock
    CurrencyService currencyService;

    @Test
    void display_ビュー名とitems属性を設定すること() {
        CurrencyController controller = new CurrencyController(currencyService);
        List<CurrencyResponseDto> items = List.of(new CurrencyResponseDto(1, "JPY", "¥"));
        when(currencyService.initialDispAll()).thenReturn(items);

        ModelAndView mav = controller.display(new ModelAndView());

        assertThat(mav.getViewName()).isEqualTo("currency-list/currency-list");
        assertThat(mav.getModel().get("items")).isEqualTo(items);
    }
}
