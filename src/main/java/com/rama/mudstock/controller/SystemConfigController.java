package com.rama.mudstock.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rama.mudstock.model.SystemConfig;
import com.rama.mudstock.service.SystemConfigService;
import com.rama.mudstock.controller.dto.SystemConfigSectionDto;

@Controller
@RequestMapping("/system-config")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping
    public String list(Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        List<SystemConfig> configs = new ArrayList<>(systemConfigService.findAllEntities());
        configs.sort(Comparator
                .comparing((SystemConfig c) -> c.getPurpose() == null ? "" : c.getPurpose(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(c -> c.getCode() == null ? "" : c.getCode(), String.CASE_INSENSITIVE_ORDER));

        Map<String, SystemConfigSectionDto> sectionsByPurpose = new LinkedHashMap<>();
        for (SystemConfig cfg : configs) {
            String code = cfg.getCode() == null ? "" : cfg.getCode().trim();
            String purpose = cfg.getPurpose() == null ? "" : cfg.getPurpose().trim();
            if (purpose.isBlank()) {
                purpose = "Uncategorized";
            }

            SystemConfigSectionDto section = sectionsByPurpose.computeIfAbsent(purpose, SystemConfigSectionDto::new);
            if ("useage".equalsIgnoreCase(code) || "usage".equalsIgnoreCase(code)) {
                String sectionDescription = cfg.getDescription();
                if (sectionDescription == null || sectionDescription.isBlank()) {
                    sectionDescription = cfg.getValue();
                }
                if (sectionDescription != null && !sectionDescription.isBlank()) {
                    // Keep the first meaningful description in case duplicate useage rows exist.
                    if (section.getDescription() == null || section.getDescription().isBlank()) {
                        section.setDescription(sectionDescription);
                    }
                }
                continue;
            }
            section.addField(cfg);
        }

        model.addAttribute("configs", configs);
        model.addAttribute("sections", new ArrayList<>(sectionsByPurpose.values()));
        return hxRequest != null ? "system_config/list :: content" : "system_config/list";
    }

    @GetMapping("/{code}/edit")
    public String editForm(@PathVariable String code,
            @RequestParam(value = "purpose", required = false) String purpose,
            Model model, RedirectAttributes ra,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        final String normalizedPurpose = normalizePurpose(purpose);
        if (normalizedPurpose == null || normalizedPurpose.isBlank()) {
            ra.addFlashAttribute("error", "Purpose is required to edit config: " + code);
            return "redirect:/system-config";
        }

        if ("useage".equalsIgnoreCase(code) || "usage".equalsIgnoreCase(code)) {
            ra.addFlashAttribute("error", "Code 'useage/usage' is display-only and not editable.");
            return "redirect:/system-config";
        }

        return systemConfigService.findEntityByPurposeAndCode(normalizedPurpose, code)
                .map(cfg -> {
                    model.addAttribute("config", cfg);
                    return hxRequest != null ? "system_config/edit :: content" : "system_config/edit";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Config not found: " + normalizedPurpose + "/" + code);
                    return "redirect:/system-config";
                });
    }

    @PostMapping("/{code}/edit")
    public String save(@PathVariable String code,
                       @RequestParam String purpose,
                       @RequestParam String value,
                       RedirectAttributes ra) {
        final String normalizedPurpose = normalizePurpose(purpose);
        if ("useage".equalsIgnoreCase(code) || "usage".equalsIgnoreCase(code)) {
            ra.addFlashAttribute("error", "Code 'useage/usage' is display-only and not editable.");
            return "redirect:/system-config";
        }

        return systemConfigService.findEntityByPurposeAndCode(normalizedPurpose, code)
                .map(cfg -> {
                    cfg.setValue(value);
                    systemConfigService.save(cfg);
                    ra.addFlashAttribute("message", "Saved config: " + normalizedPurpose + "/" + code);
                    return "redirect:/system-config";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Config not found: " + normalizedPurpose + "/" + code);
                    return "redirect:/system-config";
                });
    }

    private String normalizePurpose(String purpose) {
        if (purpose == null) {
            return null;
        }
        String trimmed = purpose.trim();
        if (trimmed.isBlank()) {
            return trimmed;
        }
        if (!trimmed.contains(",")) {
            return trimmed;
        }
        for (String part : trimmed.split(",")) {
            String candidate = part == null ? "" : part.trim();
            if (!candidate.isBlank()) {
                return candidate;
            }
        }
        return trimmed;
    }
}
