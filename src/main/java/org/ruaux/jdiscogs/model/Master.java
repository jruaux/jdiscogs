package org.ruaux.jdiscogs.model;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.List;

@Data
@XmlRootElement(name = "master")
@XmlAccessorType(XmlAccessType.FIELD)
public class Master {

    @XmlAttribute
    private Long id;
    private String title;
    private String notes;
    private String data_quality;
    private Long num_for_sale;
    private Long most_recent_release;
    private String most_recent_release_url;
    private Long main_release;
    private String main_release_url;
    private String uri;
    private String versions_url;
    private Long year;
    private Double lowest_price;
    @XmlElement(name = "image")
    @XmlElementWrapper(name = "images")
    private List<Image> images;
    @XmlElement(name = "artist")
    @XmlElementWrapper(name = "artists")
    private List<Artist> artists;
    @XmlElement(name = "genre")
    @XmlElementWrapper(name = "genres")
    private List<String> genres;
    @XmlElement(name = "style")
    @XmlElementWrapper(name = "styles")
    private List<String> styles;
    @XmlElement(name = "track")
    @XmlElementWrapper(name = "tracklist")
    private List<Track> tracklist;
    @XmlElement(name = "video")
    @XmlElementWrapper(name = "videos")
    private List<Video> videos;

    public Image getPrimaryImage() {
        if (images == null) {
            return null;
        }
        return images.stream().filter(Image::isPrimary).findFirst().orElse(null);
    }

}
