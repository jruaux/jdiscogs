package org.ruaux.jdiscogs.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NormalizedTrack implements Comparable<NormalizedTrack> {

    private String position;
    private int number;
    private Integer disc;
    private Long duration;
    private String title;
    private String artist;

    @Override
    public int compareTo(NormalizedTrack track) {
        return Integer.compare(number, track.getNumber());
    }

}
