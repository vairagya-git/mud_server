package com.rama.mudstock.controller;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.rama.mudstock.enums.SystemRepositoryEnum.OptionContractStatusEnum;
import com.rama.mudstock.facade.optiontrade.OptionTradeFacade;
import com.rama.mudstock.service.app.ApplicationMetaDataService;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping({"/option-simulator", "/option-trading", "/trade"})
public class OptionTradeController {

    private final OptionTradeFacade optionTradeFacade;
    private final ApplicationMetaDataService applicationMetaDataService;

    public OptionTradeController(OptionTradeFacade optionTradeFacade,
                                 ApplicationMetaDataService applicationMetaDataService) {
        this.optionTradeFacade = optionTradeFacade;
        this.applicationMetaDataService = applicationMetaDataService;
    }

    @GetMapping
    public String list(Model model,
                       @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        populateFilterData(model, false);
        return hxRequest != null ? "trade/strategylookup :: content" : "trade/strategylookup";
    }

    @GetMapping("/openoptiontrade")
    public String optionTrade(Model model,
                              @RequestParam(value = "expired_date_history", defaultValue = "false") boolean expiredDateHistory,
                              @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        populateFilterData(model, expiredDateHistory);
        model.addAttribute("expiredDateHistory", expiredDateHistory);
        return hxRequest != null ? "trade/openoptiontrade :: content" : "trade/openoptiontrade";
    }

    @GetMapping("/summary")
    public String summary(Model model,
                          @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        populateTradeSummaryData(model);
        return hxRequest != null ? "trade/summary :: content" : "trade/summary";
    }

    @GetMapping("/tradesummary")
    public String tradeSummary(Model model,
                               @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        populateTradeSummaryData(model);
        return hxRequest != null ? "trade/tradesummary :: content" : "trade/tradesummary";
    }

    private void populateTradeSummaryData(Model model) {
        OptionTradeFacade.TradeSummaryData summaryData = optionTradeFacade.loadTradeSummaryData();
        model.addAttribute("aliveTrades", summaryData.aliveTrades());
        model.addAttribute("optionStrategies", summaryData.optionStrategies());
        model.addAttribute("optionStrategyLegs", summaryData.optionStrategyLegs());
        model.addAttribute("optionStrategySnapshots", summaryData.optionStrategySnapshots());
        model.addAttribute("optionStrategySnapshotLegs", summaryData.optionStrategySnapshotLegs());
    }

    @GetMapping("/openoptiontrade/open-trades")
    @ResponseBody
    public List<OptionTradeFacade.OpenTradeOption> openTrades(@RequestParam Long stockId) {
        return optionTradeFacade.listOpenTradeOptions(stockId);
    }

    @GetMapping("/openoptiontrade/open-trades-by-ticker")
    @ResponseBody
    public List<OptionTradeFacade.OpenTradeOption> openTradesByTicker(@RequestParam String ticker) {
        return optionTradeFacade.listOpenTradeOptionsByTicker(ticker);
    }

    @GetMapping("/openoptiontrade/contracts")
    @ResponseBody
    public List<OptionTradeFacade.ContractOption> contracts(@RequestParam String ticker,
                                                            @RequestParam LocalDate expirationDate) {
        return optionTradeFacade.listContractsForTickerAndExpiration(ticker, expirationDate);
    }

    @GetMapping("/openoptiontrade/snapshots")
    @ResponseBody
    public List<OptionTradeFacade.SnapshotQuoteOption> snapshotQuotes(@RequestParam String contractTicker) {
        return optionTradeFacade.listSnapshotQuoteOptions(contractTicker);
    }

    @PostMapping("/openoptiontrade/open-trades")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createOpenTrade(@RequestParam Long stockId,
                                                               @RequestParam String tradeName,
                                                               @RequestParam(value = "withHistoricData", defaultValue = "false") boolean withHistoricData) {
        try {
            OptionTradeFacade.OpenTradeOption created = optionTradeFacade.createOpenTrade(stockId, tradeName, withHistoricData);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "trade", created
            ));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/openoptiontrade/strategy-snapshot")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createStrategySnapshot(@RequestBody StrategySnapshotRequest request) {
        try {
            OptionTradeFacade.StrategySnapshotCreateResult result = optionTradeFacade.createStrategySnapshot(
                request.strategyDefinitionId(),
                request.stockId(),
                request.optionTradeId(),
                request.optionTime(),
                toFacadeSelectedLegs(request.selectedLegs()));

            return ResponseEntity.ok(Map.of(
                "success", true,
                "optionStrategyId", result.optionStrategyId(),
                "optionStrategySnapshotId", result.optionStrategySnapshotId(),
                "resolvedOptionTime", result.resolvedOptionTime()
            ));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private void populateFilterData(Model model, boolean expiredDateHistory) {
        String contractStatus = expiredDateHistory ? null : OptionContractStatusEnum.ACTIVE.name() ;
        OptionTradeFacade.OptionTradeFilterData filterData = optionTradeFacade
            .loadFilterData(contractStatus);

        model.addAttribute("strategyDefinitions", filterData.strategyDefinitions());
        model.addAttribute("strategyDefinitionLegs", filterData.strategyDefinitionLegs());
        model.addAttribute("tradeTickers", filterData.tickers());
        model.addAttribute("tradeExpirationDates", filterData.expirationDates());
        model.addAttribute("tradeModes", applicationMetaDataService.listOptionTradeModes());
    }

    private List<OptionTradeFacade.SelectedLegInput> toFacadeSelectedLegs(List<SelectedLegRequest> selectedLegs) {
        if (selectedLegs == null || selectedLegs.isEmpty()) {
            return List.of();
        }

        return selectedLegs.stream()
            .map(leg -> new OptionTradeFacade.SelectedLegInput(
                leg.optionStrategyDefinitionLegId(),
                leg.contractTicker()))
            .toList();
    }

    public record StrategySnapshotRequest(Long strategyDefinitionId,
                                          Long stockId,
                                          Long optionTradeId,
                                          String optionTime,
                                          List<SelectedLegRequest> selectedLegs) {
    }

    public record SelectedLegRequest(Long optionStrategyDefinitionLegId,
                                     String contractTicker) {
    }
}
