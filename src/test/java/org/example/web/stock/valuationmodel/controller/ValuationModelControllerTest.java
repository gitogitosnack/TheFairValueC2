package org.example.web.stock.valuationmodel.controller;

import org.example.web.stock.valuationmodel.domain.ValuationModelResponseDto;
import org.example.web.stock.valuationmodel.service.ValuationModelService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValuationModelControllerTest {

    @Mock
    ValuationModelService valuationModelService;

    @Test
    void display_ビュー名とitems属性を設定すること() {
        ValuationModelController controller = new ValuationModelController(valuationModelService);
        List<ValuationModelResponseDto> items = List.of(new ValuationModelResponseDto(1, "DCF法", "説明"));
        when(valuationModelService.initialDispAll()).thenReturn(items);

        ModelAndView mav = controller.display(new ModelAndView());

        assertThat(mav.getViewName()).isEqualTo("valuation-model-list/valuation-model-list");
        assertThat(mav.getModel().get("items")).isEqualTo(items);
    }
}
