package com.rama.mudstock.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.rama.mudstock.model.Stock;
import com.rama.mudstock.model.Watchlist;
import com.rama.mudstock.repository.StockRepository;
import com.rama.mudstock.repository.WatchlistRepository;

@Controller
@RequestMapping("/stock-watchlist")
public class StockWatchlistController {
    private final StockRepository stockRepo;
    private final WatchlistRepository watchlistRepo;

    public StockWatchlistController(StockRepository stockRepo, WatchlistRepository watchlistRepo) {
        this.stockRepo = stockRepo;
        this.watchlistRepo = watchlistRepo;
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
