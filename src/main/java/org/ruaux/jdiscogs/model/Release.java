package org.ruaux.jdiscogs.model;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
public class Release {

    @XmlAttribute
    private Long id;
    @XmlAttribute
    private String status;
    private String title;
    private String country;
    private String released;
    private String notes;
    private String data_quality;
    private MasterId master_id;
    private Community community;
    private Integer year;
    private Long format_quantity;
    private String artists_sort;
    private String thumb;
    private Long num_for_sale;
    private String date_changed;
    private Float lowest_price;
    private String release_formatted;
    private Integer estimated_weight;
    private String date_added;
    private String master_url;
    private String uri;
    @XmlElement(name = "image")
    @XmlElementWrapper(name = "images")
    private List<Image> images;
    @XmlElement(name = "artist")
    @XmlElementWrapper(name = "artists")
    private List<Artist> artists;
    @XmlElement(name = "label")
    @XmlElementWrapper(name = "labels")
    private List<Label> labels;
    @XmlElement(name = "artist")
    @XmlElementWrapper(name = "extraartists")
    private List<Artist> extraartists;
    @XmlElement(name = "format")
    @XmlElementWrapper(name = "formats")
    private List<Format> formats;
    @XmlElement(name = "genre")
    @XmlElementWrapper(name = "genres")
    private List<String> genres;
    @XmlElement(name = "style")
    @XmlElementWrapper(name = "styles")
    private List<String> styles;
    @XmlElement(name = "track")
    @XmlElementWrapper(name = "tracklist")
    private List<Track> tracklist;
    @XmlElement(name = "identifier")
    @XmlElementWrapper(name = "identifiers")
    private List<Identifier> identifiers;
    @XmlElement(name = "video")
    @XmlElementWrapper(name = "videos")
    private List<Video> videos;
    @XmlElement(name = "company")
    @XmlElementWrapper(name = "companies")
    private List<Company> companies;
    @XmlElement(name = "series")
    @XmlElementWrapper(name = "series")
    private List<Series> series;

}
