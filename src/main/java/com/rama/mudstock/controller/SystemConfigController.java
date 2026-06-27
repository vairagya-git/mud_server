package com.rama.mudstock.controller;

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
        model.addAttribute("configs", systemConfigService.findAllEntities());
        return hxRequest != null ? "system_config/list :: content" : "system_config/list";
    }

    @GetMapping("/{code}/edit")
    public String editForm(@PathVariable String code, Model model, RedirectAttributes ra,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        return systemConfigService.findEntityByCode(code)
                .map(cfg -> {
                    model.addAttribute("config", cfg);
                    return hxRequest != null ? "system_config/edit :: content" : "system_config/edit";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Config not found: " + code);
                    return "redirect:/system-config";
                });
    }

    @PostMapping("/{code}/edit")
    public String save(@PathVariable String code,
                       @RequestParam String value,
                       @RequestParam String type,
                       @RequestParam String description,
                       RedirectAttributes ra) {
        return systemConfigService.findEntityByCode(code)
                .map(cfg -> {
                    cfg.setValue(value);
                    cfg.setType(type);
                    cfg.setDescription(description);
                    systemConfigService.save(cfg);
                    ra.addFlashAttribute("message", "Saved config: " + code);
                    return "redirect:/system-config";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Config not found: " + code);
                    return "redirect:/system-config";
                });
    }
}
