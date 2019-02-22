package org.ruaux.jdiscogs.api.model;

import java.util.List;

import lombok.Data;

@Data
public class Format {
	private String qty;
	private List<String> descriptions;
	private String name;

}
