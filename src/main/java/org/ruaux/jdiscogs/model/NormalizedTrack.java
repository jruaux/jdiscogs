package org.ruaux.jdiscogs.model;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.List;

@Data
@Builder
public class NormalizedTrack implements Comparable<NormalizedTrack> {

    private Position position;
    private Duration duration;
    private String title;
    private List<Artist> artists;

    @Override
    public int compareTo(NormalizedTrack track) {
        if (position == null) {
            if (track.getPosition() == null) {
                return 0;
            }
            return -1;
        }
        if (track.getPosition() == null) {
            return 1;
        }
        return position.compareTo(track.getPosition());
    }

}
