package com.rama.mudstock.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rama.mudstock.repository.analyst.FirmAnalystQueryRepository;
import com.rama.mudstock.service.BenzingaFirmService;

// Sync
@Controller
@RequestMapping("/analyst-rating")
public class AnalystRatingController {

    private final BenzingaFirmService benzingaFirmService;
    private final FirmAnalystQueryRepository firmAnalystQueryRepository;

    public AnalystRatingController(BenzingaFirmService benzingaFirmService,
                                   FirmAnalystQueryRepository firmAnalystQueryRepository) {
        this.benzingaFirmService = benzingaFirmService;
        this.firmAnalystQueryRepository = firmAnalystQueryRepository;
    }

    /** List all firms stored in the database. */
    @GetMapping("/firms")
    public String listFirms(Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("firms", benzingaFirmService.listAll());
        return hxRequest != null ? "analyst_rating/firms :: content" : "analyst_rating/firms";
    }

    /** Trigger a sync from the Benzinga API endpoint and redirect back. */
    @PostMapping("/firms/sync")
    public String syncFirms(RedirectAttributes ra) {
        try {
            int count = benzingaFirmService.fetchAndSave();
            ra.addFlashAttribute("message", "Sync complete. Upserted " + count + " firm(s).");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Sync failed: " + ex.getMessage());
        }
        return "redirect:/analyst-rating/firms";
    }

    /** List all analysts with their firm name. */
    @GetMapping("/analysts")
    public String listAnalysts(Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("analysts", firmAnalystQueryRepository.listAllAnalystsWithFirmName());
        return hxRequest != null ? "analyst_rating/analysts :: content" : "analyst_rating/analysts";
    }

    /** List analyst stock ratings, optionally filtered by ticker, analyst name, and/or firm name. */
    @GetMapping("/ratings")
    public String listRatings(Model model,
            @RequestParam(required = false) List<String> ticker,
            @RequestParam(required = false) List<String> analyst,
            @RequestParam(required = false) List<String> firm,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("ratings", firmAnalystQueryRepository.listAllRatingsWithMeta(ticker, analyst, firm));
        model.addAttribute("tickers", firmAnalystQueryRepository.listDistinctRatingTickers());
        model.addAttribute("analysts", firmAnalystQueryRepository.listDistinctRatingAnalysts());
        model.addAttribute("firms", firmAnalystQueryRepository.listDistinctRatingFirms());
        model.addAttribute("selectedTickers", ticker != null ? ticker : java.util.Collections.emptyList());
        model.addAttribute("selectedAnalysts", analyst != null ? analyst : java.util.Collections.emptyList());
        model.addAttribute("selectedFirms", firm != null ? firm : java.util.Collections.emptyList());
        return hxRequest != null ? "analyst_rating/ratings :: content" : "analyst_rating/ratings";
    }
}