package com.rama.mudstock.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rama.mudstock.master.MasterDataCache;
import com.rama.mudstock.model.MasterMarketHoliday;
import com.rama.mudstock.repository.MasterMarketHolidayRepository;

@Controller
@RequestMapping("/market_holidays")
public class MarketHolidayController {

    private final MasterMarketHolidayRepository masterMarketHolidayRepository;
    private final MasterDataCache masterDataCache;

    public MarketHolidayController(MasterMarketHolidayRepository masterMarketHolidayRepository,
                                   MasterDataCache masterDataCache) {
        this.masterMarketHolidayRepository = masterMarketHolidayRepository;
        this.masterDataCache = masterDataCache;
    }

    @GetMapping
    public String list(Model model,
                       @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("holidays", loadSortedHolidays());
        return hxRequest != null ? "market_holidays/list :: content" : "market_holidays/list";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           Model model,
                           RedirectAttributes ra,
                           @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        return masterMarketHolidayRepository.findById(id)
                .map(holiday -> {
                    model.addAttribute("holiday", holiday);
                    return hxRequest != null ? "market_holidays/edit :: content" : "market_holidays/edit";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Market holiday not found: " + id);
                    return "redirect:/market_holidays";
                });
    }

    @PostMapping("/{id}/edit")
    public String save(@PathVariable Long id,
                       @RequestParam String year,
                       @RequestParam String country,
                       @RequestParam LocalDate holidayDate,
                       RedirectAttributes ra) {
        return masterMarketHolidayRepository.findById(id)
                .map(holiday -> {
                    holiday.setYear(year == null ? null : year.trim());
                    holiday.setCountry(country == null ? null : country.trim());
                    holiday.setHolidayDate(holidayDate);
                    masterMarketHolidayRepository.save(holiday);
                    refreshHolidayCache();
                    ra.addFlashAttribute("message", "Updated market holiday: " + holidayDate);
                    return "redirect:/market_holidays";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Market holiday not found: " + id);
                    return "redirect:/market_holidays";
                });
    }

    @PostMapping("/add")
    public String add(@RequestParam String year,
                      @RequestParam String country,
                      @RequestParam LocalDate holidayDate,
                      RedirectAttributes ra) {
        MasterMarketHoliday holiday = new MasterMarketHoliday();
        holiday.setYear(year == null ? null : year.trim());
        holiday.setCountry(country == null ? null : country.trim());
        holiday.setHolidayDate(holidayDate);
        masterMarketHolidayRepository.save(holiday);
        refreshHolidayCache();
        ra.addFlashAttribute("message", "Added market holiday: " + holidayDate);
        return "redirect:/market_holidays";
    }

    private List<MasterMarketHoliday> loadSortedHolidays() {
        List<MasterMarketHoliday> holidays = new ArrayList<>(masterMarketHolidayRepository.findAll());
        holidays.sort(Comparator
                .comparing(MasterMarketHoliday::getHolidayDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(holiday -> holiday.getYear() == null ? "" : holiday.getYear(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(holiday -> holiday.getCountry() == null ? "" : holiday.getCountry(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(holiday -> holiday.getId() == null ? Long.MAX_VALUE : holiday.getId()));
        return holidays;
    }

    private void refreshHolidayCache() {
        masterDataCache.putMarketHolidays(masterMarketHolidayRepository.findAll());
    }
}