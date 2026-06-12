package com.rama.mudstock.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rama.mudstock.model.Stock;
import com.rama.mudstock.model.Watchlist;
import com.rama.mudstock.repository.StockRepository;
import com.rama.mudstock.repository.WatchlistRepository;
import com.rama.mudstock.service.AlphaVantageStockService;

@Controller
@RequestMapping("/stock-watchlist")
public class StockWatchlistController {
    private final StockRepository stockRepo;
    private final WatchlistRepository watchlistRepo;
    private final AlphaVantageStockService stockService;

    public StockWatchlistController(StockRepository stockRepo, WatchlistRepository watchlistRepo,
                                    AlphaVantageStockService stockService) {
        this.stockRepo = stockRepo;
        this.watchlistRepo = watchlistRepo;
        this.stockService = stockService;
    }

    @GetMapping
    public String view(Model model) {
        // redirect manage root to stocks list
        return "redirect:/stock-watchlist/stocks";
    }

    @GetMapping("/stocks")
    public String listStocks(Model model) {
        model.addAttribute("stocks", stockRepo.findAll());
        return "stock-watchlist/stock";
    }

    @GetMapping("/stocks/new")
    public String newStockForm(Model model) {
        return "stock-watchlist/addstock";
    }

    @GetMapping("/stocks/{ticker}/timeseries")
    @ResponseBody
    public org.springframework.http.ResponseEntity<String> getTimeSeries(@PathVariable String ticker) {
        String body = stockService.fetchDailyTimeSeries(ticker);
        return org.springframework.http.ResponseEntity.ok(body);
    }

    @GetMapping("/watchlists/new")
    public String newWatchlistForm(Model model) {
        return "stock-watchlist/addwatchlist";
    }

    @GetMapping("/watchlists")
    public String listWatchlists(Model model) {
        model.addAttribute("watchlists", watchlistRepo.findAll());
        return "stock-watchlist/watchlist";
    }

    @PostMapping("/stock")
    public String addStock(@RequestParam(required = false) String ticker,
                           @RequestParam(required = false, name = "name") String stockName,
                           @RequestParam String cusip,
                           @RequestParam String cik,
                           @RequestParam(name = "cl") String cl,
                           @RequestParam String country) {
        // required fields: cusip, cik, cl, country
        if (cusip == null || cusip.isBlank() || cik == null || cik.isBlank() || cl == null || cl.isBlank() || country == null || country.isBlank()) {
            // missing required, ignore
            return "redirect:/stock-watchlist";
        }
        String t = (ticker == null) ? null : ticker.trim().toUpperCase();
        Stock s = new Stock(t, stockName, cusip.trim(), cik.trim(), cl.trim(), country.trim());
        stockRepo.save(s);
        return "redirect:/stock-watchlist";
    }

    @PostMapping("/watchlist")
    public String addWatchlist(@RequestParam String name) {
        if (name != null && !name.isBlank()) {
            Watchlist w = new Watchlist(name.trim());
            watchlistRepo.save(w);
        }
        return "redirect:/stock-watchlist";
    }

    @PostMapping("/watchlists/addStock")
    public String addStockToWatchlist(@RequestParam Long watchlistId,
                                      @RequestParam(required = false) Long stockId,
                                      @RequestParam(required = false) String ticker,
                                      @RequestParam(required = false, name = "name") String stockName,
                                      @RequestParam(required = false) String cusip,
                                      @RequestParam(required = false) String cik,
                                      @RequestParam(required = false, name = "cl") String cl,
                                      @RequestParam(required = false) String country) {
        var maybeW = watchlistRepo.findById(watchlistId);
        if (maybeW.isPresent()) {
            Watchlist w = maybeW.get();
            Stock s = null;
            if (stockId != null) {
                s = stockRepo.findById(stockId).orElse(null);
            } else if (ticker != null && !ticker.isBlank()) {
                String t = ticker.trim().toUpperCase();
                // If stock exists by ticker reuse, otherwise create new if required fields present
                s = stockRepo.findByTicker(t).orElse(null);
                if (s == null) {
                    // require cusip,cik,cl,country to create new stock
                        if (cusip != null && cik != null && cl != null && country != null
                                && !cusip.isBlank() && !cik.isBlank() && !cl.isBlank() && !country.isBlank()) {
                            s = new Stock(t, stockName, cusip.trim(), cik.trim(), cl.trim(), country.trim());
                            stockRepo.save(s);
                        }
                }
            }
            if (s != null) {
                w.addStock(s);
                watchlistRepo.save(w);
            }
        }
        return "redirect:/stock-watchlist";
    }

    @PostMapping("/watchlists/{id}/removeStock")
    public String removeStockFromWatchlist(@PathVariable Long id, @RequestParam Long stockId) {
        var maybeW = watchlistRepo.findById(id);
        var maybeS = stockRepo.findById(stockId);
        if (maybeW.isPresent() && maybeS.isPresent()) {
            Watchlist w = maybeW.get();
            Stock s = maybeS.get();
            w.removeStock(s);
            watchlistRepo.save(w);
        }
        return "redirect:/stock-watchlist/watchlists/" + id;
    }

    // ----- Watchlist <-> Stock mapping -----

    @GetMapping("/mapping")
    public String mappingForm(Model model) {
        model.addAttribute("watchlists", watchlistRepo.findAll());
        return "stock-watchlist/mapping";
    }

    @PostMapping("/mapping")
    public String addMapping(@RequestParam Long watchlistId,
                             @RequestParam String ticker,
                             RedirectAttributes ra) {
        Watchlist w = watchlistRepo.findById(watchlistId).orElse(null);
        if (w == null) {
            ra.addFlashAttribute("error", "Watchlist not found");
            return "redirect:/stock-watchlist/mapping";
        }
        Stock s = (ticker == null || ticker.isBlank())
                ? null
                : stockRepo.findByTicker(ticker.trim().toUpperCase()).orElse(null);
        if (s == null) {
            ra.addFlashAttribute("error", "Stock ticker not found: " + ticker);
            return "redirect:/stock-watchlist/mapping";
        }
        if (w.getStocks().stream().anyMatch(x -> x.getId().equals(s.getId()))) {
            ra.addFlashAttribute("message", s.getTicker() + " is already mapped to " + w.getCode());
        } else {
            w.addStock(s);
            watchlistRepo.save(w);
            ra.addFlashAttribute("message", "Mapped " + s.getTicker() + " to " + w.getCode());
        }
        return "redirect:/stock-watchlist/mapping";
    }

    // CSV format per line: WATCHLIST_CODE;TICKER;COUNTRY  (e.g. MOVING_STOCK;LMT;USA)
    @PostMapping("/mapping/bulk")
    public String bulkMapping(@RequestParam String bulkData, RedirectAttributes ra) {
        if (bulkData == null || bulkData.isBlank()) {
            ra.addFlashAttribute("error", "No data provided");
            return "redirect:/stock-watchlist/mapping";
        }
        java.util.Map<String, Watchlist> wlCache = new java.util.HashMap<>();
        int mapped = 0, createdWatchlists = 0, duplicates = 0, malformed = 0;
        java.util.List<String> unknownTickers = new java.util.ArrayList<>();

        for (String raw : bulkData.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(";");
            if (parts.length < 2) { malformed++; continue; }
            String code = parts[0].trim();
            String ticker = parts[1].trim().toUpperCase();
            String country = parts.length >= 3 ? parts[2].trim() : null;
            if (code.isEmpty() || ticker.isEmpty()) { malformed++; continue; }

            Watchlist w = wlCache.get(code);
            if (w == null) {
                w = watchlistRepo.findByCode(code).orElse(null);
                if (w == null) {
                    w = watchlistRepo.save(new Watchlist(code, code,
                            (country != null && !country.isEmpty()) ? country : "USA"));
                    createdWatchlists++;
                }
                wlCache.put(code, w);
            }

            Stock s = stockRepo.findByTicker(ticker).orElse(null);
            if (s == null) { unknownTickers.add(ticker); continue; }

            if (w.getStocks().stream().anyMatch(x -> x.getId().equals(s.getId()))) {
                duplicates++;
                continue;
            }
            try {
                w.addStock(s);
                watchlistRepo.save(w);
                mapped++;
            } catch (Exception e) {
                w.getStocks().remove(s);
                duplicates++;
            }
        }

        StringBuilder msg = new StringBuilder();
        msg.append("Mapped ").append(mapped).append(" stock(s).");
        if (createdWatchlists > 0) msg.append(" Created ").append(createdWatchlists).append(" new watchlist(s).");
        if (duplicates > 0) msg.append(" Skipped ").append(duplicates).append(" existing mapping(s).");
        if (malformed > 0) msg.append(" Skipped ").append(malformed).append(" malformed line(s).");
        if (!unknownTickers.isEmpty()) {
            msg.append(" Unknown ticker(s): ").append(String.join(", ", unknownTickers)).append('.');
        }
        ra.addFlashAttribute("message", msg.toString());
        return "redirect:/stock-watchlist/mapping";
    }

    @GetMapping("/watchlists/{id}")
    public String viewWatchlist(@PathVariable Long id, Model model) {
        var maybeW = watchlistRepo.findById(id);
        if (maybeW.isPresent()) {
            model.addAttribute("watchlist", maybeW.get());
            model.addAttribute("allStocks", stockRepo.findAll());
            return "stock-watchlist/watchlist_detail";
        }
        return "redirect:/stock-watchlist";
    }
}
