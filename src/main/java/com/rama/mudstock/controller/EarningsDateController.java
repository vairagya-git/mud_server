package com.rama.mudstock.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.rama.mudstock.model.earnings.EarningsDate;
import com.rama.mudstock.model.earnings.EarningsDateView;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.service.EarningsDateService;
import com.rama.mudstock.util.MudDateUtil;


// Sync
@Controller
@RequestMapping("/earnings")
public class EarningsDateController {
    private final EarningsDateService service;

    public EarningsDateController(EarningsDateService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        var list = service.listAll();
        var stocks = service.allStocks();
        java.util.Map<Long,String> sym = new java.util.HashMap<>();
        for (Stock s : stocks) sym.put(s.getId(), s.getTicker());
        java.util.List<EarningsDateView> view = new java.util.ArrayList<>();
        for (var ed : list) {
            EarningsDateView v = new EarningsDateView();
            v.setId(ed.getId());
            v.setStockId(ed.getStockId());
            v.setStockSymbol(sym.get(ed.getStockId()));
            v.setQuarter(ed.getQuarter());
            v.setReleaseTime(ed.getReleaseTime());
            v.setState(ed.getStatus());
            v.setEarningsDate(ed.getEarningsDate());
            view.add(v);
        }
        model.addAttribute("list", view);
        return hxRequest != null ? "earnings/list :: content" : "earnings/list";
    }

    @GetMapping("/list")
    public String listAlias(Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        return list(model, hxRequest);
    }

    @GetMapping("/entries")
    public String entries(Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("entryList", service.listAllEntries());
        return hxRequest != null ? "earnings/entries :: content" : "earnings/entries";
    }

    @GetMapping("/new")
    public String createForm(Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        EarningsDate ed = new EarningsDate();
        model.addAttribute("ed", ed);
        model.addAttribute("stocks", service.allStocks());
        model.addAttribute("releaseOptions", EarningsDate.ReleaseTime.values());
        model.addAttribute("stateOptions", EarningsDate.Status.values());
        return hxRequest != null ? "earnings/form :: content" : "earnings/form";
    }
    @PostMapping
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam(required = false) Long stockId,
                       @RequestParam(required = false) String quarter,
                       @RequestParam(required = false) EarningsDate.ReleaseTime releaseTime,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate earningsDate) {

        // Single entry flow
        EarningsDate ed = new EarningsDate();
        ed.setId(id);
        ed.setStockId(stockId);
        ed.setQuarter(quarter);
        ed.setReleaseTime(releaseTime);
        ed.setStatus(EarningsDate.Status.NEW); // status is always NEW on save; ignore form value
        ed.setEarningsDate(earningsDate);
        service.save(ed);
        return "redirect:/earnings";
    }

    @GetMapping("/bulk")
    public String bulkForm(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        return hxRequest != null ? "earnings/bulk :: content" : "earnings/bulk";
    }

    @PostMapping("/bulk")
    public String bulkUpload(@RequestParam String bulkData) {
        if (bulkData == null || bulkData.isBlank()) return "redirect:/earnings";
        String[] lines = bulkData.split("\\r?\\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",");
            if (parts.length < 4) continue; // skip malformed
            String ticker = parts[0].trim();
            String q = parts[1].trim();
            String rtRaw = parts[2].trim();
            String dateRaw = parts[3].trim();
            try {
                // supports d/M/yyyy or d-M-yyyy — normalise slash to dash then use FMT_D_M_YYYY
                java.time.LocalDate dt = java.time.LocalDate.parse(
                        dateRaw.replace('/', '-'), MudDateUtil.FMT_D_M_YYYY);
                Stock stock = service.findOrCreateStockByTicker(ticker);
                if (stock == null) continue;
                EarningsDate ed = new EarningsDate();
                ed.setStockId(stock.getId());
                ed.setQuarter(q);
                try { ed.setReleaseTime(EarningsDate.ReleaseTime.valueOf(rtRaw)); } catch (Exception e) { ed.setReleaseTime(EarningsDate.ReleaseTime.AFTER_MARKET); }
                ed.setEarningsDate(dt);
                ed.setStatus(EarningsDate.Status.NEW);
                service.save(ed);
            } catch (Exception ex) {
                // ignore parse errors for now
            }
        }
        return "redirect:/earnings";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        EarningsDate ed = service.get(id).orElse(new EarningsDate());
        model.addAttribute("ed", ed);
        model.addAttribute("stocks", service.allStocks());
        model.addAttribute("releaseOptions", EarningsDate.ReleaseTime.values());
        model.addAttribute("stateOptions", EarningsDate.Status.values());
        return hxRequest != null ? "earnings/form :: content" : "earnings/form";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/earnings";
    }

    @GetMapping("/upcoming")
    public String upcoming(Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("upcoming", service.listUpcoming());
        model.addAttribute("today", java.time.LocalDate.now());
        model.addAttribute("twoWeeksOut", java.time.LocalDate.now().plusDays(14));
        return hxRequest != null ? "earnings/upcoming :: content" : "earnings/upcoming";
    }
}
