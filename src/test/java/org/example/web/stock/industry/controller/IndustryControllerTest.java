package org.example.web.stock.industry.controller;

import org.example.web.stock.industry.domain.IndustryResponseDto;
import org.example.web.stock.industry.service.IndustryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndustryControllerTest {

    @Mock
    IndustryService industryService;

    @Test
    void display_ビュー名とitems属性を設定すること() {
        IndustryController controller = new IndustryController(industryService);
        List<IndustryResponseDto> items = List.of(new IndustryResponseDto(1, "自動車", "輸送用機器", "説明", null));
        when(industryService.initialDispAll()).thenReturn(items);

        ModelAndView mav = controller.display(new ModelAndView());

        assertThat(mav.getViewName()).isEqualTo("industry-list/industry-list");
        assertThat(mav.getModel().get("items")).isEqualTo(items);
    }
}
