package com.rama.mudstock.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rama.mudstock.facade.OptionContractAnalysisFacade;
import com.rama.mudstock.enums.SystemRepositoryEnum.OptionIntervalAnalyseStatusEnum;
import com.rama.mudstock.enums.SystemRepositoryEnum.OptionSourceEnum;
import com.rama.mudstock.model.option.OptionContract;

@Controller
@RequestMapping("/option-analysis")
public class OptionAnalysisController {

    private final OptionContractAnalysisFacade optionContractAnalysisFacade;

    public OptionAnalysisController(OptionContractAnalysisFacade optionContractAnalysisFacade) {
        this.optionContractAnalysisFacade = optionContractAnalysisFacade;
    }

    @GetMapping("/analyse")
    public String analyseForm(Model model,
                              @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("stocks", optionContractAnalysisFacade.listOptionAnalysisStocksSorted());
        model.addAttribute("entries", optionContractAnalysisFacade.listAnalyseEntries());

        return hxRequest != null ? "option_analysis/analyse :: content" : "option_analysis/analyse";
    }

    @PostMapping("/analyse")
    public String create(@RequestParam Long stockId,
                         @RequestParam String contractType,
                         @RequestParam(defaultValue = "API") String source,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expirationDate,
                         @RequestParam BigDecimal strikeFrom,
                         @RequestParam BigDecimal strikeTo,
                         @RequestParam BigDecimal interval,
                         RedirectAttributes redirectAttributes) {
        try {
            String normalizedContractType = contractType == null ? "" : contractType.trim().toUpperCase();
            String normalizedSource = normalizeAnalyseSource(source);
            String normalizedStatus = OptionIntervalAnalyseStatusEnum.CREATE_CONTRACT.name();

            optionContractAnalysisFacade.createAnalyseEntry(
                stockId,
                normalizedContractType,
                normalizedSource,
                normalizedStatus,
                expirationDate,
                strikeFrom,
                strikeTo,
                interval);

            redirectAttributes.addFlashAttribute("message", "Option analysis entry saved.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", "Entry already exists for stock/contract/expiry/strike range or violates table constraints.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Failed to save option analysis entry: " + ex.getMessage());
        }

        return "redirect:/option-analysis/analyse";
    }

    @GetMapping("/analyse/{id}/edit")
    public String editAnalyseForm(@PathVariable Long id,
                                  Model model,
                                  RedirectAttributes redirectAttributes,
                                  @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("stocks", optionContractAnalysisFacade.listOptionAnalysisStocksSorted());

        var entry = optionContractAnalysisFacade.findAnalyseEntryById(id);
        if (entry == null) {
            redirectAttributes.addFlashAttribute("error", "Option analysis entry not found: " + id);
            return "redirect:/option-analysis/analyse";
        }

        model.addAttribute("entry", entry);
        return hxRequest != null ? "option_analysis/analyse_edit :: content" : "option_analysis/analyse_edit";
    }

    @PostMapping("/analyse/{id}/edit")
    public String updateAnalyse(@PathVariable Long id,
                                @RequestParam Long stockId,
                                @RequestParam String contractType,
                                @RequestParam String status,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expirationDate,
                                @RequestParam BigDecimal strikeFrom,
                                @RequestParam BigDecimal strikeTo,
                                @RequestParam BigDecimal interval,
                                RedirectAttributes redirectAttributes) {
        try {
            String normalizedContractType = contractType == null ? "" : contractType.trim().toUpperCase();
            String normalizedStatus = normalizeAnalyseStatus(status);

            int updated = optionContractAnalysisFacade.updateAnalyseEntry(
                id,
                stockId,
                normalizedContractType,
                normalizedStatus,
                expirationDate,
                strikeFrom,
                strikeTo,
                interval);

            if (updated == 0) {
                redirectAttributes.addFlashAttribute("error", "Option analysis entry not found: " + id);
            } else {
                redirectAttributes.addFlashAttribute("message", "Option analysis entry updated.");
            }
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", "Update violates unique/constraint rules for this entry.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Failed to update option analysis entry: " + ex.getMessage());
        }

        return "redirect:/option-analysis/analyse";
    }

    @PostMapping("/analyse/{id}/status")
    public String updateAnalyseStatus(@PathVariable Long id,
                                      @RequestParam String status,
                                      RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> entry = optionContractAnalysisFacade.findAnalyseEntryById(id);
            if (entry == null) {
                redirectAttributes.addFlashAttribute("error", "Option analysis entry not found: " + id);
                return "redirect:/option-analysis/analyse#pane-entries";
            }

            String currentStatus = entry.get("status") == null ? "" : entry.get("status").toString().trim().toUpperCase();
            String requestedStatus = normalizeAnalyseStatus(status);

            if (OptionIntervalAnalyseStatusEnum.ACTIVE.name().equals(currentStatus)
                && OptionIntervalAnalyseStatusEnum.CLOSE.name().equals(requestedStatus)) {
                optionContractAnalysisFacade.updateAnalyseStatus(id, requestedStatus);
                redirectAttributes.addFlashAttribute("message", "Status updated to CLOSE.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Only ACTIVE entries can be changed to CLOSE from this screen.");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Failed to update status: " + ex.getMessage());
        }

        return "redirect:/option-analysis/analyse#pane-entries";
    }

    private String normalizeAnalyseStatus(String status) {
        OptionIntervalAnalyseStatusEnum resolved = OptionIntervalAnalyseStatusEnum.fromValue(status);
        return resolved != null ? resolved.name() : OptionIntervalAnalyseStatusEnum.CREATE_CONTRACT.name();
    }

    private String normalizeAnalyseSource(String source) {
        OptionSourceEnum resolved = OptionSourceEnum.fromValue(source);
        return resolved != null ? resolved.name() : OptionSourceEnum.API.name();
    }

    @GetMapping("/contract")
    public String contractList(Model model,
                               @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        OptionContractAnalysisFacade.OptionContractFilterContainer filters = optionContractAnalysisFacade.loadOptionContractFilterContainer();
        model.addAttribute("contracts", java.util.List.of());
        model.addAttribute("contractTickers", filters.contractTickers());
        model.addAttribute("contractSources", filters.contractSources());
        model.addAttribute("contractExpirations", filters.contractExpirations());
        return hxRequest != null ? "option_analysis/contract :: content" : "option_analysis/contract";
    }

    @GetMapping("/contract/contracts/filter")
    @ResponseBody
    public List<OptionContract> contractContractsFilter(@RequestParam Long stockId,
                                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expirationDate,
                                                        @RequestParam(required = false) BigDecimal strikePrice,
                                                        @RequestParam(defaultValue = "BOTH") String contractType,
                                                        @RequestParam(defaultValue = "BOTH") String source) {
        String normalizedContractType = contractType == null || contractType.isBlank() ? "BOTH" : contractType.trim().toUpperCase();
        String normalizedSource = source == null || source.isBlank() ? "BOTH" : source.trim().toUpperCase();
        return optionContractAnalysisFacade.listContractsForContractSelection(stockId, expirationDate, strikePrice, normalizedContractType, normalizedSource);
    }

    @GetMapping("/snapshot")
    public String snapshotList(Model model,
                               @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        OptionContractAnalysisFacade.OptionContractFilterContainer filters = optionContractAnalysisFacade.loadOptionContractFilterContainer();
        model.addAttribute("activeContractTickers", filters.contractTickers());
        model.addAttribute("contractExpirations", filters.contractExpirations());
        model.addAttribute("snapshotRefreshIntervalMs", filters.snapshotRefreshIntervalMs());
        return hxRequest != null ? "option_analysis/snapshot :: content" : "option_analysis/snapshot";
    }

    @GetMapping("/metrics")
    public String metricsList(Model model,
                              @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("metrics", optionContractAnalysisFacade.listMetrics());
        return hxRequest != null ? "option_analysis/metrics :: content" : "option_analysis/metrics";
    }

    @GetMapping("/snapshot/strikes")
    @ResponseBody
    public List<OptionContract> snapshotStrikeOptions(@RequestParam Long stockId,
                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expirationDate) {
        return optionContractAnalysisFacade.listStrikeOptions(stockId, expirationDate);
    }

    @GetMapping("/snapshot/contracts/filter")
    @ResponseBody
    public List<OptionContract> snapshotContractsForStrike(@RequestParam Long stockId,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expirationDate,
                                                           @RequestParam BigDecimal strikePrice) {
        return optionContractAnalysisFacade.listContractsWithSnapshotFlatFileForStrike(stockId, expirationDate, strikePrice);
    }

}

//Changed For Git