package org.ruaux.jdiscogs.model;

import java.util.List;

import lombok.Data;

@Data
public class Master {

	private List<String> styles;
	private List<String> genres;
	private String mainReleaseUrl;
	private Integer numForSale;
	private List<Video> videos;
	private String title;
	private Long mainRelease;
	private String notes;
	private List<Artist> artists;
	private String uri;
	private String versionsUrl;
	private Float lowestPrice;
	private Integer year;
	private List<Image> images;
	private String resourceUrl;
	private List<Track> tracklist;
	private Long id;
	private String dataQuality;

}
