package com.rama.mudstock.controller.dto;

import java.util.ArrayList;
import java.util.List;

import com.rama.mudstock.model.SystemConfig;

public class SystemConfigSectionDto {

	private final String title;
	private String description;
	private final List<SystemConfig> fields = new ArrayList<>();

	public SystemConfigSectionDto(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<SystemConfig> getFields() {
		return fields;
	}

	public void addField(SystemConfig config) {
		fields.add(config);
	}
}
