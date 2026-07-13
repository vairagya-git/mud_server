package com.rama.mudstock.controller;

import java.util.Comparator;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rama.mudstock.enums.OptionStrategyEnum;
import com.rama.mudstock.repository.option.OptionStrategyRepository;
import com.rama.mudstock.repository.stockwatchlist.StockRepository;

@Controller
@RequestMapping("/option-strategy")
public class OptionStrategyController {

    private final StockRepository stockRepository;
    private final OptionStrategyRepository optionStrategyRepository;

    public OptionStrategyController(StockRepository stockRepository,
                                    OptionStrategyRepository optionStrategyRepository) {
        this.stockRepository = stockRepository;
        this.optionStrategyRepository = optionStrategyRepository;
    }

    @GetMapping
    public String list(Model model,
                       @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        var stocks = stockRepository.findAll();
        stocks.sort(Comparator.comparing(s -> s.getTicker() == null ? "" : s.getTicker(), String.CASE_INSENSITIVE_ORDER));

        model.addAttribute("stocks", stocks);
        model.addAttribute("strategies", optionStrategyRepository.listAllWithTicker());
        model.addAttribute("strategyNames", optionStrategyRepository.listStrategyNameOptions());
        model.addAttribute("strategyTypes", OptionStrategyEnum.StrategyType.values());
        model.addAttribute("strategyModes", OptionStrategyEnum.StrategyMode.values());
        model.addAttribute("strategyActions", OptionStrategyEnum.StrategyAction.values());
        model.addAttribute("strategyStatuses", OptionStrategyEnum.StrategyStatus.values());

        return hxRequest != null ? "option_strategy/list :: content" : "option_strategy/list";
    }

    @PostMapping
    public String create(@RequestParam Long stockId,
                         @RequestParam(required = false) Long previousStrategyId,
                         @RequestParam String strategyName,
                         @RequestParam String strategyType,
                         @RequestParam String strategyMode,
                         @RequestParam String strategyAction,
                         @RequestParam String status,
                         RedirectAttributes redirectAttributes) {
        try {
            String normalizedStrategyName = normalizeText(strategyName);
            String normalizedStrategyType = normalizeEnum(strategyType);
            String normalizedStrategyMode = normalizeEnum(strategyMode);
            String normalizedStrategyAction = normalizeEnum(strategyAction);
            String normalizedStatus = normalizeEnum(status);

            optionStrategyRepository.insert(
                stockId,
                previousStrategyId,
                normalizedStrategyName,
                normalizedStrategyType,
                normalizedStrategyMode,
                normalizedStrategyAction,
                normalizedStatus);

            redirectAttributes.addFlashAttribute("message", "Option strategy saved.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", "Strategy already exists or violates table constraints.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Failed to save option strategy: " + ex.getMessage());
        }

        return "redirect:/option-strategy";
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEnum(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}