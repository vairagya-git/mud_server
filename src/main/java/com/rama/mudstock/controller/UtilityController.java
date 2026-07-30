package com.rama.mudstock.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.rama.mudstock.service.PathUpdateService;
import com.rama.mudstock.service.S3OptionFlatfileService;

@Controller
@RequestMapping("/utility")
public class UtilityController {

    private final PathUpdateService pathUpdateService;
    private final S3OptionFlatfileService s3OptionFlatfileService;

    public UtilityController(PathUpdateService pathUpdateService,
                             S3OptionFlatfileService s3OptionFlatfileService) {
        this.pathUpdateService = pathUpdateService;
        this.s3OptionFlatfileService = s3OptionFlatfileService;
    }

    @GetMapping("/path-update")
    public String pathUpdateForm(Model model,
                                 @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        return hxRequest != null ? "utility/path_update :: content" : "utility/path_update";
    }

    @GetMapping("/path-update/test")
    public String pathUpdateTest(Model model,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 @RequestParam(required = false) String ticker,
                                 @RequestParam(required = false, defaultValue = "100") Integer limit,
                                 @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        LocalDate resolvedDate = date == null ? s3OptionFlatfileService.getConfiguredTestDay() : date;
        String resolvedTicker = (ticker == null || ticker.isBlank())
            ? s3OptionFlatfileService.getConfiguredTestTicker()
            : ticker.trim();
        int safeLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 2000));

        model.addAttribute("selectedDate", resolvedDate);
        model.addAttribute("selectedTicker", resolvedTicker);
        model.addAttribute("selectedLimit", safeLimit);

        try {
            model.addAttribute("bucketInfo", s3OptionFlatfileService.fetchBucketUpdateTimestamp(resolvedDate));
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
        }

        try {
            model.addAttribute("optionData", s3OptionFlatfileService.fetchOptionRows(resolvedDate, resolvedTicker, safeLimit));
        } catch (Exception ex) {
            String existingError = (String) model.getAttribute("error");
            String optionError = "Failed to load option rows: " + ex.getMessage();
            model.addAttribute("error", existingError == null ? optionError : existingError + " | " + optionError);
        }

        return hxRequest != null ? "utility/path_update_test :: content" : "utility/path_update_test";
    }

    @GetMapping("/csv-opt")
    public String csvOpt(Model model,
                         @RequestParam(required = false) String fileLocation,
                         @RequestParam(required = false, defaultValue = "window_start") String sortBy,
                         @RequestParam(required = false, defaultValue = "asc") String sortDirection,
                         @RequestParam(required = false, defaultValue = "100") Integer limit,
                         @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        String resolvedFileLocation = fileLocation == null ? "" : fileLocation.trim();
        String resolvedSortBy = (sortBy == null || sortBy.isBlank()) ? "window_start" : sortBy.trim();
        String resolvedSortDirection = (sortDirection == null || sortDirection.isBlank()) ? "asc" : sortDirection.trim();
        int safeLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 2000));

        model.addAttribute("selectedFileLocation", resolvedFileLocation);
        model.addAttribute("selectedSortBy", resolvedSortBy);
        model.addAttribute("selectedSortDirection", resolvedSortDirection);
        model.addAttribute("selectedLimit", safeLimit);

        if (!resolvedFileLocation.isBlank()) {
            try {
                model.addAttribute("csvOptData", s3OptionFlatfileService.fetchCsvOptRows(
                    resolvedFileLocation,
                    resolvedSortBy,
                    resolvedSortDirection,
                    safeLimit));
            } catch (Exception ex) {
                model.addAttribute("error", ex.getMessage());
            }
        }

        return hxRequest != null ? "utility/csv_opt :: content" : "utility/csv_opt";
    }

    @PostMapping("/path-update")
    public String runPathUpdate(@RequestParam String folderPath,
                                @RequestParam String findString,
                                @RequestParam String replaceString,
                                Model model,
                                @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("folderPath", folderPath);
        model.addAttribute("findString", findString);
        model.addAttribute("replaceString", replaceString);

        if (folderPath == null || folderPath.isBlank()) {
            model.addAttribute("error", "Folder location is required.");
            return hxRequest != null ? "utility/path_update :: content" : "utility/path_update";
        }
        if (findString == null || findString.isBlank()) {
            model.addAttribute("error", "Find string is required.");
            return hxRequest != null ? "utility/path_update :: content" : "utility/path_update";
        }

        Path folder;
        try {
            folder = Path.of(folderPath);
        } catch (Exception ex) {
            model.addAttribute("error", "Invalid folder path.");
            return hxRequest != null ? "utility/path_update :: content" : "utility/path_update";
        }

        if (!folder.isAbsolute()) {
            model.addAttribute("error", "Folder location must be an absolute path.");
            return hxRequest != null ? "utility/path_update :: content" : "utility/path_update";
        }
        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            model.addAttribute("error", "Folder does not exist or is not a directory.");
            return hxRequest != null ? "utility/path_update :: content" : "utility/path_update";
        }

        try {
            var result = pathUpdateService.replaceInFolder(folder, findString, replaceString == null ? "" : replaceString);
            model.addAttribute("message", "Path update completed.");
            model.addAttribute("result", result);
        } catch (Exception ex) {
            model.addAttribute("error", "Failed to run path update: " + ex.getMessage());
        }

        return hxRequest != null ? "utility/path_update :: content" : "utility/path_update";
    }

    @GetMapping("/s3-option-records")
    @ResponseBody
    public Map<String, Object> fetchS3OptionRecords(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) String ticker,
        @RequestParam(defaultValue = "20") int limit) {

        LocalDate resolvedDate = date == null ? s3OptionFlatfileService.getConfiguredTestDay() : date;
        String resolvedTicker = (ticker == null || ticker.isBlank())
            ? s3OptionFlatfileService.getConfiguredTestTicker()
            : ticker;
        int safeLimit = Math.max(1, Math.min(limit, 500));

        return s3OptionFlatfileService.fetchOptionRows(resolvedDate, resolvedTicker, safeLimit);
    }
}
