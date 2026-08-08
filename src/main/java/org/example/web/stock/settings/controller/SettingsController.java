package org.example.web.stock.settings.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    @GetMapping("")
    public ModelAndView display(ModelAndView mav) {
        mav.setViewName("settings/settings");
        return mav;
    }
}
