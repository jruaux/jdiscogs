package org.ruaux.jdiscogs.api.model;

import lombok.Data;

@Data
public class Label {
	private String name;
	private String entityType;
	private String catno;
	private String resourceUrl;
	private Long id;
	private String entityTypeName;
}
