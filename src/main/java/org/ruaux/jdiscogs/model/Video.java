package org.ruaux.jdiscogs.model;

import lombok.Getter;
import lombok.Setter;

public class Video {

	@Getter
	@Setter
	private Integer duration;
	@Getter
	@Setter
	private Boolean embed;
	@Getter
	@Setter
	private String title;
	@Getter
	@Setter
	private String description;
	@Getter
	@Setter
	private String uri;

}
