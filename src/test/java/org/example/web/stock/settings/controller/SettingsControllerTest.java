package org.example.web.stock.settings.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsControllerTest {

    @Test
    void display_ビュー名を設定すること() {
        SettingsController controller = new SettingsController();

        ModelAndView mav = controller.display(new ModelAndView());

        assertThat(mav.getViewName()).isEqualTo("settings/settings");
    }
}
