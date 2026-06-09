package com.rama.mudstock.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.rama.mudstock.model.DayEventMaster;
import com.rama.mudstock.model.Stock;
import com.rama.mudstock.repository.DayEventEntryRepository;
import com.rama.mudstock.repository.DayEventMappingRepository;
import com.rama.mudstock.repository.DayEventMasterRepository;
import com.rama.mudstock.repository.StockRepository;

@Controller
@RequestMapping("/day-event")
public class DayEventController {
    private final DayEventMasterRepository repo;
    private final DayEventMappingRepository mappingRepo;
    private final StockRepository stockRepo;
    private final DayEventEntryRepository entryRepo;

    public DayEventController(DayEventMasterRepository repo, DayEventMappingRepository mappingRepo, StockRepository stockRepo, DayEventEntryRepository entryRepo) {
        this.repo = repo;
        this.mappingRepo = mappingRepo;
        this.stockRepo = stockRepo;
        this.entryRepo = entryRepo;
    }

    // Day event master list and create
    @GetMapping
    public String list(Model model) {
        model.addAttribute("events", repo.findAll());
        model.addAttribute("newEvent", new DayEventMaster());
        return "day_event/dayeventmaster";
    }

    @PostMapping
    public String create(@RequestParam String code,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate event_date) {
        if (code == null || code.isBlank()) {
            return "redirect:/day-event";
        }
        DayEventMaster dem = new DayEventMaster(code.trim(), description == null ? null : description.trim(), event_date);
        repo.save(dem);
        return "redirect:/day-event";
    }

    // Day event mappings (list, add, bulk)
    @GetMapping("/mapping")
    public String listMappings(Model model) {
        List<Map<String,Object>> mappings = mappingRepo.listAllMappings();
        model.addAttribute("mappings", mappings);
        List<Stock> stocks = stockRepo.findAll();
        List<DayEventMaster> days = repo.findAll();
        model.addAttribute("stocks", stocks);
        model.addAttribute("days", days);
        return "day_event/dayeventmapping";
    }

    @PostMapping("/mapping")
    public String addMapping(@RequestParam(required = false) String ticker,
                      @RequestParam(required = false) String code) {
        if (ticker == null || ticker.isBlank() || code == null || code.isBlank()) return "redirect:/day-event/mapping";
        Optional<Stock> sopt = stockRepo.findByTicker(ticker.trim().toUpperCase());
        Optional<DayEventMaster> dopt = repo.findAll().stream().filter(d->code.trim().equals(d.getCode())).findFirst();
        if (sopt.isPresent() && dopt.isPresent()) {
            mappingRepo.createMapping(sopt.get().getId(), dopt.get().getId());
        }
        return "redirect:/day-event/mapping";
    }

    @GetMapping("/mapping/bulk")
    public String bulkForm() { return "day_event/dayeventmapping_bulk"; }

    @PostMapping("/mapping/bulk")
    public String bulkUpload(@RequestParam String bulkData) {
        if (bulkData == null || bulkData.isBlank()) return "redirect:/day-event/mapping";
        String[] lines = bulkData.split("\\r?\\n");
        List<Stock> stocks = stockRepo.findAll();
        List<DayEventMaster> days = repo.findAll();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",");
            if (parts.length < 2) continue;
            String ticker = parts[0].trim().toUpperCase();
            String code = parts[1].trim();
            Optional<Stock> sopt = stocks.stream().filter(s->ticker.equals(s.getTicker())).findFirst();
            Optional<DayEventMaster> dopt = days.stream().filter(d->code.equals(d.getCode())).findFirst();
            if (sopt.isPresent() && dopt.isPresent()) {
                try { mappingRepo.createMapping(sopt.get().getId(), dopt.get().getId()); } catch (Exception e) { /* ignore duplicates/errors */ }
            }
        }
        return "redirect:/day-event/mapping";
    }

    // Day event entries listing
    @GetMapping("/entries")
    public String listEntries(Model model) {
        model.addAttribute("entries", entryRepo.listAllEntriesWithMeta());
        return "day_event/dayevententries";
    }
}
