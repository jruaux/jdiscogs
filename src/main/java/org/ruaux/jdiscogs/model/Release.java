package org.ruaux.jdiscogs.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.expression.spel.ast.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Release extends IdResource {

    @Getter
    @Setter
    private String status;
    @Getter
    @Setter
    private List<Entity> series;
    @Getter
    @Setter
    private List<Entity> labels;
    @Getter
    @Setter
    private Community community;
    @Setter
    private Integer year;
    @Getter
    @Setter
    private List<Image> images;
    @Getter
    @Setter
    private Long format_quantity;
    @Getter
    @Setter
    private List<Video> videos;
    @Getter
    @Setter
    private String artists_sort;
    @Getter
    @Setter
    private List<String> genres;
    @Getter
    @Setter
    private String thumb;
    @Getter
    @Setter
    private Long num_for_sale;
    @Getter
    @Setter
    private String title;
    @Getter
    @Setter
    private List<Artist> artists;
    @Getter
    @Setter
    private String date_changed;
    @Getter
    @Setter
    private Float lowest_price;
    @Getter
    @Setter
    private List<String> styles;
    @Getter
    @Setter
    private Long master_id;
    @Getter
    @Setter
    private String release_formatted;
    @Getter
    @Setter
    private List<Format> formats;
    @Getter
    @Setter
    private Integer estimated_weight;
    @Getter
    @Setter
    private String released;
    @Getter
    @Setter
    private String date_added;
    @Getter
    @Setter
    private List<Artist> extraartists;
    @Getter
    @Setter
    private List<Track> tracklist;
    @Getter
    @Setter
    private String notes;
    @Getter
    @Setter
    private List<Identifier> identifiers;
    @Getter
    @Setter
    private List<Entity> companies;
    @Getter
    @Setter
    private String master_url;
    @Getter
    @Setter
    private String uri;
    @Getter
    @Setter
    private String country;
    @Getter
    @Setter
    private String data_quality;


    public List<Track> getTracks() {
        return getTracks(tracklist);
    }

    private List<Track> getTracks(List<Track> tracks) {
        List<Track> actualTracks = new ArrayList<>();
        for (Track track : tracks) {
            if (track.getSub_tracks().isEmpty()) {
                actualTracks.add(track);
            } else {
                actualTracks.addAll(getTracks(track.getSub_tracks()));
            }
        }
        return actualTracks;
    }

    public String getArtist() {
        if (artists == null || artists.isEmpty()) {
            return null;
        }
        return artists.get(0).getName();
    }

    public Format getFormat() {
        if (formats == null || formats.isEmpty()) {
            return null;
        }
        return formats.get(0);
    }

    public String getStyle() {
        if (styles == null || styles.isEmpty()) {
            return null;
        }
        return styles.get(0);
    }

    public String getGenre() {
        if (genres == null || genres.isEmpty()) {
            return null;
        }
        return genres.get(0);
    }

    public List<Track> getTracks(Integer disc) {
        if (disc == null) {
            return getTracks();
        }
        return getTracks().stream().filter(t -> disc.equals(t.getDisc())).collect(Collectors.toList());
    }

    public Integer getYear() {
        if (year == null) {
            if (released.length() < 4) {
                return null;
            }
            return Integer.parseInt(released.substring(0, 4));
        }
        return year;
    }

}
