package com.rama.mudstock.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import com.rama.mudstock.service.DayStockMovementService;

@Controller
@RequestMapping("/day-stock-movement")
public class DayStockMovementController {
    private final DayStockMovementService dayStockMovementService;

    public DayStockMovementController(DayStockMovementService dayStockMovementService) {
        this.dayStockMovementService = dayStockMovementService;
    }

    @GetMapping
    public String list(Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        return showEntries(model, hxRequest);
    }

    @GetMapping("/entries")
    public String entries(Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        return showEntries(model, hxRequest);
    }

    private String showEntries(Model model, String hxRequest) {
        model.addAttribute("entries", dayStockMovementService.listEntriesWithMeta());
        model.addAttribute("tickers", dayStockMovementService.listDistinctEntryTickers());
        return hxRequest != null ? "day_stock_movement/day_stock_movement_entries :: content" : "day_stock_movement/day_stock_movement_entries";
    }
}
