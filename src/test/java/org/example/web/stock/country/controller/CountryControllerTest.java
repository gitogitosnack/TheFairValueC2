package org.example.web.stock.country.controller;

import org.example.web.stock.country.domain.CountryResponseDto;
import org.example.web.stock.country.service.CountryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountryControllerTest {

    @Mock
    CountryService countryService;

    @Test
    void display_ビュー名とitems属性を設定すること() {
        CountryController controller = new CountryController(countryService);
        List<CountryResponseDto> items = List.of(new CountryResponseDto(1, "JP", "日本"));
        when(countryService.initialDispAll()).thenReturn(items);

        ModelAndView mav = controller.display(new ModelAndView());

        assertThat(mav.getViewName()).isEqualTo("country-list/country-list");
        assertThat(mav.getModel().get("items")).isEqualTo(items);
    }
}
