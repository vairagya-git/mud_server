package com.rama.mudstock.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import com.rama.mudstock.facade.OptionTradeFacade;

@Controller
@RequestMapping({"/option-simulator", "/option-trading", "/trade"})
public class OptionTradeController {

    private final OptionTradeFacade optionTradeFacade;

    public OptionTradeController(OptionTradeFacade optionTradeFacade) {
        this.optionTradeFacade = optionTradeFacade;
    }

    @GetMapping
    public String list(Model model,
                       @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        OptionTradeFacade.OptionTradeFilterData filterData = optionTradeFacade.loadFilterData();
        model.addAttribute("strategyDefinitions", filterData.strategyDefinitions());
        model.addAttribute("strategyDefinitionLegs", filterData.strategyDefinitionLegs());
        model.addAttribute("tradeTickers", filterData.tickers());
        model.addAttribute("tradeExpirationDates", filterData.expirationDates());
        model.addAttribute("tradeContracts", filterData.contracts());

        return hxRequest != null ? "trade/list :: content" : "trade/list";
    }
}
