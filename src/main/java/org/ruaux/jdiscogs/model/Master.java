package org.ruaux.jdiscogs.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class Master extends IdResource {

	@Getter
	@Setter
	private List<String> styles;
	@Getter
	@Setter
	private List<String> genres;
	@Getter
	@Setter
	private Long num_for_sale;
	@Getter
	@Setter
	private String title;
	@Getter
	@Setter
	private Long most_recent_release;
	@Getter
	@Setter
	private String most_recent_release_url;
	@Getter
	@Setter
	private Long main_release;
	@Getter
	@Setter
	private String main_release_url;
	@Getter
	@Setter
	private String uri;
	@Getter
	@Setter
	private List<Artist> artists;
	@Getter
	@Setter
	private String versions_url;
	@Getter
	@Setter
	private String data_quality;
	@Getter
	@Setter
	private Long year;
	@Getter
	@Setter
	private List<Image> images;
	@Getter
	@Setter
	private Double lowest_price;
	@Getter
	@Setter
	private List<Track> tracklist;
	@Getter
	@Setter
	private List<Video> videos;
	@Getter
	@Setter
	private String notes;

	public Image getPrimaryImage() {
		if (images==null) {
			return null;
		}
		return images.stream().filter(image -> image.isPrimary()).findFirst().orElse(null);
	}

}
