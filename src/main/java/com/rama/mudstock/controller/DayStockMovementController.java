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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rama.mudstock.model.daystock.DayStockMovementKey;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.model.stockwatchlist.Watchlist;
import com.rama.mudstock.repository.daystock.DayStockMovementEntryRepository;
import com.rama.mudstock.repository.daystock.DayStockMovementMapRepository;
import com.rama.mudstock.repository.daystock.DayStockMovementKeyRepository;
import com.rama.mudstock.repository.stockwatchlist.StockRepository;
import com.rama.mudstock.repository.stockwatchlist.WatchlistRepository;
import com.rama.mudstock.service.MarketCalendarService;
import com.rama.mudstock.util.MudDateUtil;

@Controller
@RequestMapping("/day-stock-movement")
public class DayStockMovementController {
    private final DayStockMovementKeyRepository repo;
    private final DayStockMovementMapRepository mappingRepo;
    private final StockRepository stockRepo;
    private final DayStockMovementEntryRepository entryRepo;
    private final WatchlistRepository watchlistRepo;
    private final com.rama.mudstock.service.DayStockMovementService dayStockMovementService;
    private final MarketCalendarService marketCalendarService;

    public DayStockMovementController(DayStockMovementKeyRepository repo,
                                      DayStockMovementMapRepository mappingRepo,
                                      StockRepository stockRepo,
                                      DayStockMovementEntryRepository entryRepo,
                                      WatchlistRepository watchlistRepo,
                                      com.rama.mudstock.service.DayStockMovementService dayStockMovementService,
                                      MarketCalendarService marketCalendarService) {
        this.repo = repo;
        this.mappingRepo = mappingRepo;
        this.stockRepo = stockRepo;
        this.entryRepo = entryRepo;
        this.watchlistRepo = watchlistRepo;
        this.dayStockMovementService = dayStockMovementService;
        this.marketCalendarService = marketCalendarService;
    }

    // Day stock movement key list and create
    @GetMapping
    public String list(Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("events", repo.findAll());
        model.addAttribute("newEvent", new DayStockMovementKey());
        return hxRequest != null ? "day_stock_movement/day_stock_movement_key :: content" : "day_stock_movement/day_stock_movement_key";
    }

    @PostMapping("/bulk")
    public String bulkCsvUpload(@RequestParam String csvData, RedirectAttributes ra) {
        if (csvData == null || csvData.isBlank()) {
            ra.addFlashAttribute("csvError", "No data provided.");
            return "redirect:/day-stock-movement";
        }
        int created = 0;
        int skipped = 0;
        String[] lines = csvData.split("\\r?\\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",", 3);
            if (parts.length < 3) { skipped++; continue; }
            String code    = parts[0].trim();
            String description = MudDateUtil.unquote(parts[1]);
            String dateStr     = MudDateUtil.unquote(parts[2]);
            if (code.isEmpty() || dateStr.isEmpty()) { skipped++; continue; }
            LocalDate date = MudDateUtil.parseFlexible(dateStr);
            if (date == null) { skipped++; continue; }
            try {
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
    public String listMappings(Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        List<Map<String,Object>> mappings = mappingRepo.listAllMappings();
        model.addAttribute("mappings", mappings);
        List<Stock> stocks = stockRepo.findAll();
        List<DayStockMovementKey> days = repo.findAll();
        model.addAttribute("stocks", stocks);
        model.addAttribute("days", days);
        return hxRequest != null ? "day_stock_movement/day_stock_movement_map :: content" : "day_stock_movement/day_stock_movement_map";
    }

    @PostMapping("/mapping")
    public String addMapping(@RequestParam(required = false) String ticker,
                             @RequestParam(required = false) String code,
                             RedirectAttributes ra) {
        if (ticker == null || ticker.isBlank() || code == null || code.isBlank()) return "redirect:/day-stock-movement/mapping";
        Optional<Stock> sopt = stockRepo.findByTicker(ticker.trim().toUpperCase());
        Optional<DayStockMovementKey> dopt = repo.findAll().stream().filter(d -> code.trim().equals(d.getCode())).findFirst();
        if (dopt.isPresent()) {
            DayStockMovementKey key = dopt.get();
            if (key.getDate() != null && marketCalendarService.isMarketClosed(key.getDate())) {
                mappingRepo.deleteEntriesByMasterId(key.getId());
                mappingRepo.deleteMappingsByMasterId(key.getId());
                mappingRepo.deleteMasterById(key.getId());
                ra.addFlashAttribute("marketClosedWarning",
                    "Market was closed on " + key.getDate() + " for key '" + key.getCode() + "'. Key and its mappings have been deleted.");
                return "redirect:/day-stock-movement/mapping";
            }
            if (sopt.isPresent()) {
                mappingRepo.createMapping(sopt.get().getId(), key.getId());
            }
        }
        return "redirect:/day-stock-movement/mapping";
    }

    @GetMapping("/mapping/bulk")
    public String bulkForm(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        return hxRequest != null ? "day_stock_movement/day_stock_movement_map_bulk :: content" : "day_stock_movement/day_stock_movement_map_bulk";
    }

    @PostMapping("/mapping/bulk")
    public String bulkUpload(@RequestParam String bulkData, RedirectAttributes ra) {
        if (bulkData == null || bulkData.isBlank()) return "redirect:/day-stock-movement/mapping";
        String[] lines = bulkData.split("\\r?\\n");
        List<Stock> stocks = stockRepo.findAll();
        List<DayStockMovementKey> days = repo.findAll();
        int created = 0, skipped = 0, deleted = 0;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",");
            if (parts.length < 2) { skipped++; continue; }
            String ticker = parts[0].trim().toUpperCase();
            String code = parts[1].trim();
            Optional<Stock> sopt = stocks.stream().filter(s -> ticker.equals(s.getTicker())).findFirst();
            Optional<DayStockMovementKey> dopt = days.stream().filter(d -> code.equals(d.getCode())).findFirst();
            if (dopt.isPresent()) {
                DayStockMovementKey key = dopt.get();
                if (key.getDate() != null && marketCalendarService.isMarketClosed(key.getDate())) {
                    mappingRepo.deleteEntriesByMasterId(key.getId());
                    mappingRepo.deleteMappingsByMasterId(key.getId());
                    mappingRepo.deleteMasterById(key.getId());
                    days = repo.findAll(); // refresh after delete
                    deleted++;
                    continue;
                }
                if (sopt.isPresent()) {
                    try { mappingRepo.createMapping(sopt.get().getId(), key.getId()); created++; } catch (Exception e) { skipped++; }
                } else { skipped++; }
            } else { skipped++; }
        }
        String msg = "Mapped " + created + " stock(s).";
        if (deleted > 0) msg += " Deleted " + deleted + " market-closed key(s).";
        if (skipped > 0) msg += " Skipped " + skipped + " line(s).";
        ra.addFlashAttribute("bulkMessage", msg);
        return "redirect:/day-stock-movement/mapping";
    }

    // Day stock movement entries listing
    @GetMapping("/entries")
    public String listEntries(Model model,
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) String dayCode,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("entries", entryRepo.listEntriesWithMeta(ticker, dayCode));
        model.addAttribute("tickers", entryRepo.listDistinctEntryTickers());
        model.addAttribute("dayCodes", entryRepo.listDistinctEntryCodes());
        model.addAttribute("selectedTicker", ticker);
        model.addAttribute("selectedDayCode", dayCode);
        return hxRequest != null ? "day_stock_movement/day_stock_movement_entries :: content" : "day_stock_movement/day_stock_movement_entries";
    }

    @GetMapping("/populate-watchlist")
    public String populateWatchlistForm(Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        List<Watchlist> watchlists = watchlistRepo.findAll();
        List<DayStockMovementKey> allDays = repo.findAll();

        // Filter out entries where the market was closed (weekend or holiday)
        List<DayStockMovementKey> activeDays = new java.util.ArrayList<>();
        List<String> removedEntries = new java.util.ArrayList<>();
        for (DayStockMovementKey key : allDays) {
            if (key.getDate() != null && marketCalendarService.isMarketClosed(key.getDate())) {
                String reason = marketCalendarService.isWeekend(key.getDate()) ? "weekend" : "holiday/vacation";
                removedEntries.add(key.getCode() + " [" + key.getDate() + "] — removed as it is a " + reason);
            } else {
                activeDays.add(key);
            }
        }

        model.addAttribute("watchlists", watchlists);
        model.addAttribute("days", activeDays);
        model.addAttribute("everydayWatchlistCode", dayStockMovementService.getWatchlistCode());
        if (!removedEntries.isEmpty()) {
            model.addAttribute("marketClosedRemovedCount", removedEntries.size());
            model.addAttribute("marketClosedRemovedEntries", removedEntries);
        }
        return hxRequest != null ? "day_stock_movement/day_stock_movement_watchlist :: content" : "day_stock_movement/day_stock_movement_watchlist";
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
