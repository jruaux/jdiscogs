package org.ruaux.jdiscogs.api.model;

import lombok.Data;

@Data
public class Image {
	private String uri;
	private String uri150;
	private String type;
	private int height;
	private int width;
}