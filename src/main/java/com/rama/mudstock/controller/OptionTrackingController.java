package com.rama.mudstock.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.rama.mudstock.facade.OptionTrackingFacade;
import com.rama.mudstock.model.option.OptionTrackingSnapshotRow;
import com.rama.mudstock.service.ApplicationFilterService;

@Controller
@RequestMapping("/option-analysis/tracking")
public class OptionTrackingController {

    private static final Logger log = LoggerFactory.getLogger(OptionTrackingController.class);

    private final OptionTrackingFacade optionTrackingFacade;
    private final ApplicationFilterService applicationFilterService;

    public OptionTrackingController(OptionTrackingFacade optionTrackingFacade,
                                    ApplicationFilterService applicationFilterService) {
        this.optionTrackingFacade = optionTrackingFacade;
        this.applicationFilterService = applicationFilterService;
    }

    @GetMapping
    public String trackingPage(Model model,
                               @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("stocks", applicationFilterService.listOptionTrackingStocksSorted());
        return hxRequest != null ? "option_analysis/tracking :: content" : "option_analysis/tracking";
    }

    @GetMapping("/expirations")
    public String expirationOptions(@RequestParam String ticker, Model model) {
        log.info("OptionTrackingController: expirationOptions called with ticker={}", ticker);
        Map<LocalDate, Long> expirations = applicationFilterService.listOptionTrackingExpirations(ticker);
        log.info("OptionTrackingController: expirationOptions resolved {} expiration(s) for ticker={} -> {}",
            expirations.size(), ticker, expirations);
        model.addAttribute("expirations", expirations);
        return "option_analysis/tracking :: expirationOptions";
    }

    @GetMapping("/contracts")
    public String contractOptions(@RequestParam Long optionsIntervalAnalyseId, Model model) {
        Map<String, Long> contractTickerIds = applicationFilterService.listOptionTrackingContractTickerIds(optionsIntervalAnalyseId);
        model.addAttribute("contractTickerIds", contractTickerIds);
        return "option_analysis/tracking :: contractOptions";
    }

    @GetMapping("/snapshots")
    @ResponseBody
    public List<OptionTrackingSnapshotRow> trackingSnapshots(@RequestParam List<Long> contractIds) {
        if (contractIds == null || contractIds.size() < 2 || contractIds.size() > 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select between 2 and 6 contracts");
        }
        return optionTrackingFacade.getTrackingSnapshotRows(contractIds);
    }
}
