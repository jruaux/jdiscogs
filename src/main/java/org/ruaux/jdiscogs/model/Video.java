package org.ruaux.jdiscogs.model;

import lombok.Data;

@Data
public class Video {
	
	private Integer duration;
	private Boolean embed;
	private String title;
	private String description;
	private String uri;

}
