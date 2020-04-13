package org.ruaux.jdiscogs.api.model;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class Release {

	private Long id;
	private String status;
	private List<Video> videos;
	private List<String> series;
	private List<Label> labels;
	private Integer year;
	private Community community;
	private List<Artist> artists;
	private List<Image> images;
	private Integer formatQuantity;
	private String artistsSort;
	private List<String> genres;
	private String thumb;
	private Integer numForSale;
	private String title;
	private Date dateChanged;
	private Long masterId;
	private Float lowestPrice;
	private List<String> styles;
	private List<Format> formats;
	private String masterUrl;
	private Date dateAdded;
	private List<Artist> extraartists;
	private List<Track> tracklist;
	private String notes;
	private List<String> identifiers;
	private List<Company> companies;
	private String uri;
	private String country;
	private String resourceUrl;
	private String dataQuality;

}
