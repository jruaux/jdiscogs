package org.ruaux.jdiscogs.model;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
public class Release {

    private static final Pattern RELEASED_PATTERN = Pattern.compile("^(?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})");

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

    public List<Track> normalizedTracks() {
        if (tracklist == null) {
            return null;
        }
        SortedMap<Track.Position, Track> map = new TreeMap<>();
        for (Track track : tracklist) {
            Track.Position position = track.position();
            if (position != null) {
                if (position.getSub() == null) {
                    map.put(position, track.toBuilder().build());
                } else {
                    position = position.toBuilder().sub(null).build();
                    Track compositeTrack = map.containsKey(position)?map.get(position):Track.builder().position(position.toString()).build();
                    List<Track> sub_tracks = compositeTrack.getSub_tracks();
                    if (sub_tracks == null) {
                        sub_tracks = new ArrayList<>();
                    }
                    sub_tracks.add(track.toBuilder().build());
                    compositeTrack.setSub_tracks(sub_tracks);
                    map.put(position, compositeTrack);
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    public Integer year() {
        if (year == null) {
            if (released == null) {
                return null;
            }
            Matcher matcher = RELEASED_PATTERN.matcher(released);
            if (matcher.matches()) {
                String year = matcher.group("year");
                if (year != null) {
                    return Integer.parseInt(year);
                }
            }
            return null;
        }
        return year;
    }

}
