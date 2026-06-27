package com.rama.mudstock.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.rama.mudstock.service.EarningsDateService;

@Controller
public class HomeController {
    private final EarningsDateService earningsService;

    public HomeController(EarningsDateService earningsService) {
        this.earningsService = earningsService;
    }

    @GetMapping("/")
    public String index(Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("stockCount", earningsService.allStocks().size());
        return hxRequest != null ? "index :: content" : "index";
    }

     @GetMapping("/index2")
    public String index2(Model model) {
        // provide counts or other small metadata if desired
        model.addAttribute("stockCount", earningsService.allStocks().size());
        return "index2";
    }
}
