package com.rama.mudstock.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
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

import com.rama.mudstock.config.ApplicationProperties;
import com.rama.mudstock.repository.option.OptionContractRepository;
import com.rama.mudstock.repository.option.OptionSnapshotIVMetricRepository;
import com.rama.mudstock.repository.option.OptionSnapshotRepository;
import com.rama.mudstock.repository.option.OptionToAnalyseRepository;
import com.rama.mudstock.repository.stockwatchlist.StockRepository;
import com.rama.mudstock.util.MudDateUtil;

@Controller
@RequestMapping("/option-analysis")
public class OptionAnalysisController {

    private final StockRepository stockRepository;
    private final OptionToAnalyseRepository optionToAnalyseRepository;
    private final OptionContractRepository optionContractRepository;
    private final OptionSnapshotRepository optionSnapshotRepository;
    private final OptionSnapshotIVMetricRepository optionSnapshotIVMetricRepository;
    private final ApplicationProperties applicationProperties;

    public OptionAnalysisController(StockRepository stockRepository,
                                    OptionToAnalyseRepository optionToAnalyseRepository,
                                    OptionContractRepository optionContractRepository,
                                    OptionSnapshotRepository optionSnapshotRepository,
                                    OptionSnapshotIVMetricRepository optionSnapshotIVMetricRepository,
                                    ApplicationProperties applicationProperties) {
        this.stockRepository = stockRepository;
        this.optionToAnalyseRepository = optionToAnalyseRepository;
        this.optionContractRepository = optionContractRepository;
        this.optionSnapshotRepository = optionSnapshotRepository;
        this.optionSnapshotIVMetricRepository = optionSnapshotIVMetricRepository;
        this.applicationProperties = applicationProperties;
    }

    @GetMapping("/analyse")
    public String analyseForm(Model model,
                              @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        var stocks = stockRepository.findAll();
        stocks.sort(Comparator.comparing(s -> s.getTicker() == null ? "" : s.getTicker(), String.CASE_INSENSITIVE_ORDER));

        model.addAttribute("stocks", stocks);
        model.addAttribute("entries", optionToAnalyseRepository.getOptionsInternalAnalyseByStatus(null));

        return hxRequest != null ? "option_analysis/analyse :: content" : "option_analysis/analyse";
    }

    @PostMapping("/analyse")
    public String create(@RequestParam Long stockId,
                         @RequestParam String contractType,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expirationDate,
                         @RequestParam BigDecimal strikeFrom,
                         @RequestParam BigDecimal strikeTo,
                         @RequestParam BigDecimal interval,
                         RedirectAttributes redirectAttributes) {
        try {
            String normalizedContractType = contractType == null ? "" : contractType.trim().toUpperCase();
            String normalizedStatus = OptionToAnalyseRepository.STATUS_CREATE_CONTRACT;

            optionToAnalyseRepository.insert(
                stockId,
                normalizedContractType,
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
        var stocks = stockRepository.findAll();
        stocks.sort(Comparator.comparing(s -> s.getTicker() == null ? "" : s.getTicker(), String.CASE_INSENSITIVE_ORDER));
        model.addAttribute("stocks", stocks);

        var entry = optionToAnalyseRepository.findByIdWithTicker(id);
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

            int updated = optionToAnalyseRepository.updateById(
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
            Map<String, Object> entry = optionToAnalyseRepository.findByIdWithTicker(id);
            if (entry == null) {
                redirectAttributes.addFlashAttribute("error", "Option analysis entry not found: " + id);
                return "redirect:/option-analysis/analyse#pane-entries";
            }

            String currentStatus = entry.get("status") == null ? "" : entry.get("status").toString().trim().toUpperCase();
            String requestedStatus = normalizeAnalyseStatus(status);

            if (OptionToAnalyseRepository.STATUS_ACTIVE.equals(currentStatus)
                && OptionToAnalyseRepository.STATUS_CLOSE.equals(requestedStatus)) {
                optionToAnalyseRepository.updateStatusById(id, requestedStatus);
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
        String normalized = status == null
            ? OptionToAnalyseRepository.STATUS_CREATE_CONTRACT
            : status.trim().toUpperCase();

        if (OptionToAnalyseRepository.STATUS_CREATE_CONTRACT.equals(normalized)
            || OptionToAnalyseRepository.STATUS_ACTIVE.equals(normalized)
            || OptionToAnalyseRepository.STATUS_PARTIALLY_COMPLETED.equals(normalized)
            || OptionToAnalyseRepository.STATUS_CLOSE.equals(normalized)
            || OptionToAnalyseRepository.STATUS_COMPLETED.equals(normalized)) {
            return normalized;
        }

        return OptionToAnalyseRepository.STATUS_CREATE_CONTRACT;
    }

    @GetMapping("/contract")
    public String contractList(Model model,
                               @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("contracts", optionContractRepository.getOptionContractsWithTickerByStatus(null, false));
        model.addAttribute("contractTickers", listDistinctContractTickers(null));
        return hxRequest != null ? "option_analysis/contract :: content" : "option_analysis/contract";
    }

    @GetMapping("/snapshot")
    public String snapshotList(Model model,
                               @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("activeContracts", optionContractRepository.getOptionContractsWithTickerByStatus(
            OptionContractRepository.STATUS_ACTIVE,
            false));
        model.addAttribute("activeContractTickers", listDistinctContractTickers(OptionContractRepository.STATUS_ACTIVE));
        model.addAttribute("snapshotRefreshIntervalMs", applicationProperties.getSnapshotRefreshMs());
        return hxRequest != null ? "option_analysis/snapshot :: content" : "option_analysis/snapshot";
    }

    @GetMapping("/metrics")
    public String metricsList(Model model,
                              @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("metrics", optionSnapshotIVMetricRepository.listAllWithTickerAndContract());
        return hxRequest != null ? "option_analysis/metrics :: content" : "option_analysis/metrics";
    }

    @GetMapping("/snapshot/contracts/{contractId}")
    @ResponseBody
    public List<Map<String, Object>> snapshotByContract(@PathVariable Long contractId) {
        List<Map<String, Object>> rows = optionSnapshotRepository.listByContractId(contractId);
        rows.forEach(row -> row.put(
            "option_quote_time",
            MudDateUtil.utcToLocalDateTimeMinuteString(row.get("option_quote_time"))));
        return rows;
    }

    private List<String> listDistinctContractTickers(String status) {
        return optionContractRepository.listDistinctTickersByStatus(status);
    }
}