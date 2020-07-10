package org.ruaux.jdiscogs;

import lombok.Setter;
import org.ruaux.jdiscogs.model.NormalizedTrack;
import org.ruaux.jdiscogs.model.Position;
import org.ruaux.jdiscogs.model.Release;
import org.ruaux.jdiscogs.model.Track;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReleaseUtils {

    private static final Pattern POSITION_PATTERN = Pattern.compile("^((?<side>[A-Z])|((?<disc>\\d+)[\\-.:]))?(?<number>\\d+)(?<sub>[a-z])?");
    private static final Pattern DURATION_PATTERN = Pattern.compile("^(?<minutes>\\d+):(?<seconds>\\d+)");
    private static final Pattern RELEASED_PATTERN = Pattern.compile("^(?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})");
    private static final Pattern CD_PATTERN = Pattern.compile("(disc|disk|cd)\\s*(?<disc>\\d+).*", Pattern.CASE_INSENSITIVE);
    public static final String DEFAULT_TRACK_SEPARATOR = " / ";
    @Setter
    private String trackSeparator = DEFAULT_TRACK_SEPARATOR;

    private static Position position(String string) {
        if (string == null) {
            return null;
        }
        Matcher matcher = POSITION_PATTERN.matcher(string);
        if (!matcher.matches()) {
            return null;
        }
        Position position = new Position();
        String number = matcher.group("number");
        if (number != null) {
            position.setNumber(Integer.parseInt(number));
        }
        String sub = matcher.group("sub");
        if (sub != null) {
            position.setSub(sub.charAt(0));
        }
        String disc = matcher.group("disc");
        if (disc != null) {
            position.setDisc(Integer.parseInt(disc));
        }
        String side = matcher.group("side");
        if (side != null) {
            position.setSide(side.charAt(0));
        }
        return position;
    }

    public static Duration duration(String string) {
        if (string == null) {
            return null;
        }
        Matcher matcher = DURATION_PATTERN.matcher(string.trim());
        if (!matcher.matches()) {
            return null;
        }
        int minutes = Integer.parseInt(matcher.group("minutes"));
        int seconds = Integer.parseInt(matcher.group("seconds"));
        return Duration.ofMinutes(minutes).plus(Duration.ofSeconds(seconds));
    }

    public List<NormalizedTrack> normalizedTracks(Release release) {
        List<NormalizedTrack> tracks = new ArrayList<>();
        if (release.getTracklist() != null) {
            for (Track track : release.getTracklist()) {
                tracks.add(normalizedTrack(track));
                if (track.getSub_tracks() != null) {
                    for (Track subTrack : track.getSub_tracks()) {
                        tracks.add(normalizedTrack(subTrack));
                    }
                }
            }
        }
        List<NormalizedTrack> toRemove = new ArrayList<>();
        Integer disc = null;
        for (NormalizedTrack track : tracks) {
            if (track.getPosition() == null) {
                Matcher matcher = CD_PATTERN.matcher(track.getTitle());
                if (matcher.matches()) {
                    disc = Integer.parseInt(matcher.group("disc"));
                }
                toRemove.add(track);
            } else {
                if (disc != null) {
                    track.getPosition().setDisc(disc);
                }
                if (track.getPosition().getSub() != null) {
                    NormalizedTrack firstSubTrack = firstSubTrack(tracks, track.getPosition());
                    if (firstSubTrack != track) {
                        firstSubTrack.setTitle(firstSubTrack.getTitle() + trackSeparator + track.getTitle());
                        toRemove.add(track);
                    }
                }
            }
        }
        tracks.removeAll(toRemove);
        return tracks;
    }

    public static NormalizedTrack firstSubTrack(List<NormalizedTrack> tracks, Position position) {
        for (NormalizedTrack track : tracks) {
            if (track.getPosition() != null && Objects.equals(track.getPosition().getDisc(), position.getDisc()) && Objects.equals(track.getPosition().getNumber(), position.getNumber()) && Objects.equals(track.getPosition().getSide(), position.getSide())) {
                return track;
            }
        }
        return null;
    }

    public static NormalizedTrack normalizedTrack(Track track) {
        return NormalizedTrack.builder().artists(track.getArtists()).duration(duration(track.getDuration())).position(position(track.getPosition())).title(track.getTitle()).build();
    }

    public static Integer year(Release release) {
        if (release.getYear() != null) {
            return release.getYear();
        }
        if (release.getReleased() == null) {
            return null;
        }
        Matcher matcher = RELEASED_PATTERN.matcher(release.getReleased());
        if (!matcher.matches()) {
            return null;
        }
        return Integer.parseInt(matcher.group("year"));
    }
}
