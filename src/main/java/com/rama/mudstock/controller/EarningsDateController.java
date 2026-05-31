package com.rama.mudstock.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.rama.mudstock.model.EarningsDate;
import com.rama.mudstock.model.Stock;
import com.rama.mudstock.service.EarningsDateService;

@Controller
@RequestMapping("/earnings")
public class EarningsDateController {
    private final EarningsDateService service;

    public EarningsDateController(EarningsDateService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        var list = service.listAll();
        var stocks = service.allStocks();
        java.util.Map<Long,String> sym = new java.util.HashMap<>();
        for (Stock s : stocks) sym.put(s.getId(), s.getTicker());
        java.util.List<com.rama.mudstock.model.EarningsDateView> view = new java.util.ArrayList<>();
        for (var ed : list) {
            com.rama.mudstock.model.EarningsDateView v = new com.rama.mudstock.model.EarningsDateView();
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
        return "earnings/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        EarningsDate ed = new EarningsDate();
        model.addAttribute("ed", ed);
        model.addAttribute("stocks", service.allStocks());
        model.addAttribute("releaseOptions", EarningsDate.ReleaseTime.values());
        model.addAttribute("stateOptions", EarningsDate.Status.values());
        return "earnings/form";
    }

    @PostMapping
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam Long stockId,
                       @RequestParam(required = false) String quarter,
                       @RequestParam EarningsDate.ReleaseTime releaseTime,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate earningsDate) {
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

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        EarningsDate ed = service.get(id).orElse(new EarningsDate());
        model.addAttribute("ed", ed);
        model.addAttribute("stocks", service.allStocks());
        model.addAttribute("releaseOptions", EarningsDate.ReleaseTime.values());
        model.addAttribute("stateOptions", EarningsDate.Status.values());
        return "earnings/form";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/earnings";
    }
}
