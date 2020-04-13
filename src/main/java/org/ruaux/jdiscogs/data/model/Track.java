package org.ruaux.jdiscogs.data.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@XmlRootElement(name = "track")
@XmlAccessorType(XmlAccessType.FIELD)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public @Data
class Track {

    private static final Pattern DURATION_PATTERN = Pattern.compile("^(?<minutes>\\d+):(?<seconds>\\d+)");
    private static final Pattern POSITION_PATTERN = Pattern.compile("^((?<side>[A-Z])|((?<disc>\\d+)[\\-.:]))?(?<number>\\d+)(?<sub>[a-z])?\\.?$");

    @XmlElement(name = "position")
    private String position;
    @XmlElement(name = "title")
    private String title;
    @XmlElement(name = "duration")
    private String duration;
    @XmlElement(name = "extraartists")
    private Artists extraartists;
    @XmlElement(name = "artists")
    private Artists artists;
    @XmlElement(name = "sub_tracks")
    private SubTrackList subTrackList;

    public String getArtist() {
        if (artists == null || artists.getArtists() == null || artists.getArtists().isEmpty()) {
            return null;
        }
        return artists.getArtists().get(0).getName();
    }

    public int getDurationInSeconds() {
        Matcher matcher = DURATION_PATTERN.matcher(duration.trim());
        if (matcher.matches()) {
            try {
                return Integer.parseInt(matcher.group("minutes")) * 60 + Integer.parseInt(matcher.group("seconds"));
            } catch (NumberFormatException e) {
                log.error("Could not parse duration '{}'", duration);
            }
        }
        return 0;
    }

    private Matcher positionMatcher() {
        return POSITION_PATTERN.matcher(position);
    }

    public String getSubPosition() {
        Matcher matcher = positionMatcher();
        if (matcher.matches()) {
            return matcher.group("sub");
        }
        return null;
    }

    public Integer getDisc() {
        Matcher matcher = positionMatcher();
        if (matcher.matches()) {
            String disc = matcher.group("disc");
            if (disc != null) {
                return Integer.parseInt(disc);
            }
        }
        return null;
    }

    public Character getSide() {
        Matcher matcher = positionMatcher();
        if (matcher.matches()) {
            String side = matcher.group("side");
            if (side != null) {
                return side.charAt(0);
            }
        }
        return null;
    }

    public Integer getNumber() {
        Matcher matcher = positionMatcher();
        if (matcher.matches()) {
            String number = matcher.group("number");
            if (number != null) {
                return Integer.parseInt(number);
            }
        }
        return null;
    }


}
