package com.rama.mudstock.controller;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.rama.mudstock.service.PathUpdateService;

@Controller
@RequestMapping("/utility")
public class UtilityController {

    private final PathUpdateService pathUpdateService;

    public UtilityController(PathUpdateService pathUpdateService) {
        this.pathUpdateService = pathUpdateService;
    }

    @GetMapping("/path-update")
    public String pathUpdateForm(Model model,
                                 @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        return hxRequest != null ? "utility/path_update :: content" : "utility/path_update";
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
}
