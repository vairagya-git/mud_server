package com.rama.mudstock.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rama.mudstock.service.BenzingaFirmService;

@Controller
@RequestMapping("/analyst-rating")
public class AnalystRatingController {

    private final BenzingaFirmService benzingaFirmService;

    public AnalystRatingController(BenzingaFirmService benzingaFirmService) {
        this.benzingaFirmService = benzingaFirmService;
    }

    /** List all firms stored in the database. */
    @GetMapping("/firms")
    public String listFirms(Model model) {
        model.addAttribute("firms", benzingaFirmService.listAll());
        return "analyst_rating/firms";
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
}