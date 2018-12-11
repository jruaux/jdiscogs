package org.ruaux.jdiscogs.api;

import java.util.List;

import lombok.Data;

@Data
public class Format {
	private String qty;
	private List<String> descriptions;
	private String name;

}
