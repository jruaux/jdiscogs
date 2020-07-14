package org.ruaux.jdiscogs.model;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.List;

@Data
@XmlRootElement(name = "track")
@XmlAccessorType(XmlAccessType.FIELD)
public class Track {

    private String position;
    private String title;
    private String duration;
    private String type_;
    @XmlElement(name = "artist")
    @XmlElementWrapper(name = "artists")
    private List<Artist> artists;
    @XmlElement(name = "track")
    @XmlElementWrapper(name = "sub_tracks")
    private List<Track> sub_tracks;

}