package com.rama.mudstock.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.time.format.DateTimeFormatter;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rama.mudstock.model.DayStockMovementKey;
import com.rama.mudstock.model.Stock;
import com.rama.mudstock.model.Watchlist;
import com.rama.mudstock.repository.DayStockMovementEntryRepository;
import com.rama.mudstock.repository.DayStockMovementMapRepository;
import com.rama.mudstock.repository.DayStockMovementKeyRepository;
import com.rama.mudstock.repository.StockRepository;
import com.rama.mudstock.repository.WatchlistRepository;

@Controller
@RequestMapping("/day-stock-movement")
public class DayStockMovementController {
    private final DayStockMovementKeyRepository repo;
    private final DayStockMovementMapRepository mappingRepo;
    private final StockRepository stockRepo;
    private final DayStockMovementEntryRepository entryRepo;
    private final WatchlistRepository watchlistRepo;
    private final com.rama.mudstock.service.DayStockMovementService dayStockMovementService;

    public DayStockMovementController(DayStockMovementKeyRepository repo,
                                      DayStockMovementMapRepository mappingRepo,
                                      StockRepository stockRepo,
                                      DayStockMovementEntryRepository entryRepo,
                                      WatchlistRepository watchlistRepo,
                                      com.rama.mudstock.service.DayStockMovementService dayStockMovementService) {
        this.repo = repo;
        this.mappingRepo = mappingRepo;
        this.stockRepo = stockRepo;
        this.entryRepo = entryRepo;
        this.watchlistRepo = watchlistRepo;
        this.dayStockMovementService = dayStockMovementService;
    }

    // Day stock movement key list and create
    @GetMapping
    public String list(Model model) {
        model.addAttribute("events", repo.findAll());
        model.addAttribute("newEvent", new DayStockMovementKey());
        return "day_stock_movement/day_stock_movement_key";
    }

    @PostMapping("/bulk")
    public String bulkCsvUpload(@RequestParam String csvData, RedirectAttributes ra) {
        if (csvData == null || csvData.isBlank()) {
            ra.addFlashAttribute("csvError", "No data provided.");
            return "redirect:/day-stock-movement";
        }
        DateTimeFormatter fmtSlash = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmtIso   = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
        int created = 0;
        int skipped = 0;
        String[] lines = csvData.split("\\r?\\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",", 3);
            if (parts.length < 3) { skipped++; continue; }
            String code    = parts[0].trim();
            String description = parts[1].trim().replaceAll("^\"|\"$", ""); // strip surrounding quotes
            String dateStr = parts[2].trim().replaceAll("^\"|\"$", "");
            if (code.isEmpty() || dateStr.isEmpty()) { skipped++; continue; }
            try {
                LocalDate date;
                if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    date = LocalDate.parse(dateStr, fmtIso);
                } else {
                    date = LocalDate.parse(dateStr, fmtSlash);
                }
                repo.save(new DayStockMovementKey(code, description, date));
                created++;
            } catch (Exception e) {
                skipped++;
            }
        }
        ra.addFlashAttribute("csvMessage", "Imported " + created + " record(s)." + (skipped > 0 ? " Skipped " + skipped + " invalid line(s)." : ""));
        return "redirect:/day-stock-movement";
    }

    @PostMapping
    public String create(@RequestParam String code,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate event_date) {
        if (code == null || code.isBlank()) {
            return "redirect:/day-stock-movement";
        }
        DayStockMovementKey key = new DayStockMovementKey(code.trim(), description == null ? null : description.trim(), event_date);
        repo.save(key);
        return "redirect:/day-stock-movement";
    }

    // Day stock movement mappings (list, add, bulk)
    @GetMapping("/mapping")
    public String listMappings(Model model) {
        List<Map<String,Object>> mappings = mappingRepo.listAllMappings();
        model.addAttribute("mappings", mappings);
        List<Stock> stocks = stockRepo.findAll();
        List<DayStockMovementKey> days = repo.findAll();
        model.addAttribute("stocks", stocks);
        model.addAttribute("days", days);
        return "day_stock_movement/day_stock_movement_map";
    }

    @PostMapping("/mapping")
    public String addMapping(@RequestParam(required = false) String ticker,
                      @RequestParam(required = false) String code) {
        if (ticker == null || ticker.isBlank() || code == null || code.isBlank()) return "redirect:/day-stock-movement/mapping";
        Optional<Stock> sopt = stockRepo.findByTicker(ticker.trim().toUpperCase());
        Optional<DayStockMovementKey> dopt = repo.findAll().stream().filter(d->code.trim().equals(d.getCode())).findFirst();
        if (sopt.isPresent() && dopt.isPresent()) {
            mappingRepo.createMapping(sopt.get().getId(), dopt.get().getId());
        }
        return "redirect:/day-stock-movement/mapping";
    }

    @GetMapping("/mapping/bulk")
    public String bulkForm() { return "day_stock_movement/day_stock_movement_map_bulk"; }

    @PostMapping("/mapping/bulk")
    public String bulkUpload(@RequestParam String bulkData) {
        if (bulkData == null || bulkData.isBlank()) return "redirect:/day-stock-movement/mapping";
        String[] lines = bulkData.split("\\r?\\n");
        List<Stock> stocks = stockRepo.findAll();
        List<DayStockMovementKey> days = repo.findAll();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",");
            if (parts.length < 2) continue;
            String ticker = parts[0].trim().toUpperCase();
            String code = parts[1].trim();
            Optional<Stock> sopt = stocks.stream().filter(s->ticker.equals(s.getTicker())).findFirst();
            Optional<DayStockMovementKey> dopt = days.stream().filter(d->code.equals(d.getCode())).findFirst();
            if (sopt.isPresent() && dopt.isPresent()) {
                try { mappingRepo.createMapping(sopt.get().getId(), dopt.get().getId()); } catch (Exception e) { /* ignore duplicates/errors */ }
            }
        }
        return "redirect:/day-stock-movement/mapping";
    }

    // Day stock movement entries listing
    @GetMapping("/entries")
    public String listEntries(Model model) {
        model.addAttribute("entries", entryRepo.listAllEntriesWithMeta());
        return "day_stock_movement/day_stock_movement_entries";
    }

    @GetMapping("/populate-watchlist")
    public String populateWatchlistForm(Model model) {
        List<Watchlist> watchlists = watchlistRepo.findAll();
        List<DayStockMovementKey> days = repo.findAll();
        model.addAttribute("watchlists", watchlists);
        model.addAttribute("days", days);
        model.addAttribute("everydayWatchlistCode", dayStockMovementService.getWatchlistCode());
        return "day_stock_movement/populate_watchlist";
    }

    @PostMapping("/populate-watchlist/by-date")
    public String populateByDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 RedirectAttributes ra) {
        var result = dayStockMovementService.populateForDate(date);
        if (!result.watchlistFound()) {
            ra.addFlashAttribute("error", "Watchlist '" + result.watchlistCode()
                    + "' not found or everyday-watchlist-code not configured");
        } else {
            ra.addFlashAttribute("message", "Day stock movement key '" + result.masterCode() + "' ready. Mapped "
                    + result.mappingsCreated() + " of " + result.stocksTotal() + " stock(s) from watchlist "
                    + result.watchlistCode() + ".");
        }
        return "redirect:/day-stock-movement/populate-watchlist";
    }

    @PostMapping("/populate-watchlist/bulk")
    public String populateWatchlistBulk(@RequestParam String csvData, RedirectAttributes ra) {
        if (csvData == null || csvData.isBlank()) {
            ra.addFlashAttribute("csvError", "No data provided.");
            return "redirect:/day-stock-movement/populate-watchlist";
        }
        int created = 0;
        int skipped = 0;
        for (String raw : csvData.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",", 2);
            if (parts.length < 2) { skipped++; continue; }
            String watchlistCode = parts[0].trim();
            String eventCode = parts[1].trim();
            if (watchlistCode.isEmpty() || eventCode.isEmpty()) { skipped++; continue; }
            Optional<Watchlist> wopt = watchlistRepo.findByCode(watchlistCode);
            Optional<DayStockMovementKey> dopt = repo.findAll().stream()
                    .filter(d -> eventCode.equals(d.getCode())).findFirst();
            if (wopt.isEmpty() || dopt.isEmpty()) { skipped++; continue; }
            Watchlist w = wopt.get();
            Long keyId = dopt.get().getId();
            for (Stock s : w.getStocks()) {
                try { mappingRepo.createMapping(s.getId(), keyId); created++; }
                catch (Exception e) { /* ignore duplicates */ }
            }
        }
        ra.addFlashAttribute("csvMessage", "Created " + created + " mapping(s)." +
                (skipped > 0 ? " Skipped " + skipped + " unresolved line(s)." : ""));
        return "redirect:/day-stock-movement/populate-watchlist";
    }

    @PostMapping("/populate-watchlist")
    public String populateWatchlistSubmit(@RequestParam Long watchlistId, @RequestParam Long dayEventMasterId, RedirectAttributes ra) {
        var maybe = watchlistRepo.findById(watchlistId);
        if (maybe.isEmpty()) {
            ra.addFlashAttribute("error", "Watchlist not found");
            return "redirect:/day-stock-movement/populate-watchlist";
        }
        Watchlist w = maybe.get();
        int created = 0;
        for (Stock s : w.getStocks()) {
            try {
                mappingRepo.createMapping(s.getId(), dayEventMasterId);
                created++;
            } catch (Exception e) {
                // ignore duplicates/errors
            }
        }
        ra.addFlashAttribute("message", "Created " + created + " mappings from watchlist " + w.getName());
        return "redirect:/day-stock-movement/mapping";
    }
}
