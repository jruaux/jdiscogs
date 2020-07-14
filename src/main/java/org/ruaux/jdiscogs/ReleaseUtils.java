package org.ruaux.jdiscogs;

import org.ruaux.jdiscogs.model.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReleaseUtils {

    private static final String TYPE_PRIMARY = "primary";
    private static final Pattern POSITION_PATTERN = Pattern.compile("^((?<side>[A-Z])|((?<disc>\\d+)[\\-.:]))?(?<number>\\d+)(?<sub>[a-z])?");
    private static final Pattern DURATION_PATTERN = Pattern.compile("^(?<minutes>\\d+):(?<seconds>\\d+)");
    private static final Pattern RELEASED_PATTERN = Pattern.compile("^(?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})");
    private static final Pattern CD_PATTERN = Pattern.compile("(disc|disk|cd)\\s*(?<disc>\\d+).*", Pattern.CASE_INSENSITIVE);
    public final static String CD = "CD";
    public final static String VINYL = "Vinyl";

    public static boolean isCd(Release release) {
        if (release.getFormats() == null || release.getFormats().isEmpty()) {
            return false;
        }
        return isCd(release.getFormats().get(0));
    }

    public static boolean isVinyl(Release release) {
        if (release.getFormats() == null || release.getFormats().isEmpty()) {
            return false;
        }
        return isVinyl(release.getFormats().get(0));
    }

    public static boolean isCd(Format format) {
        return CD.equalsIgnoreCase(format.getName());
    }

    public static boolean isVinyl(Format format) {
        return VINYL.equalsIgnoreCase(format.getName());
    }

    public static final String TRACK_SEPARATOR = " / ";

    private static Position position(String string) {
        if (string == null) {
            return null;
        }
        Matcher matcher = POSITION_PATTERN.matcher(string);
        if (!matcher.matches()) {
            return null;
        }
        Position position = new Position();
        position.setString(string);
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

    public static List<NormalizedTrack> normalizedTracks(Release release) {
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
        int trackNumber = 1;
        for (NormalizedTrack track : tracks) {
            if (track.getPosition() == null) {
                Matcher matcher = CD_PATTERN.matcher(track.getTitle());
                if (matcher.matches()) {
                    disc = Integer.parseInt(matcher.group("disc"));
                    trackNumber = 1;
                }
                toRemove.add(track);
            } else {
                if (disc != null) {
                    track.getPosition().setDisc(disc);
                    track.getPosition().setNumber(trackNumber);
                }
                if (track.getPosition().getSub() != null) {
                    NormalizedTrack firstSubTrack = firstSubTrack(tracks, track.getPosition());
                    if (firstSubTrack != track) {
                        assert firstSubTrack != null;
                        firstSubTrack.setTitle(firstSubTrack.getTitle() + TRACK_SEPARATOR + track.getTitle());
                        toRemove.add(track);
                    }
                }
                trackNumber++;
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
        NormalizedTrack normalizedTrack = new NormalizedTrack();
        normalizedTrack.setArtists(track.getArtists());
        normalizedTrack.setDuration(duration(track.getDuration()));
        normalizedTrack.setPosition(position(track.getPosition()));
        normalizedTrack.setTitle(track.getTitle());
        return normalizedTrack;
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

    public static boolean isPrimary(Image image) {
        return TYPE_PRIMARY.equals(image.getType());
    }

    public static Double ratio(Image image) {
        if (image.getHeight() == null) {
            return null;
        }
        if (image.getWidth() == null) {
            return null;
        }
        return image.getHeight().doubleValue() / image.getWidth().doubleValue();
    }

    public static Image primaryImage(Master master) {
        if (master.getImages() == null) {
            return null;
        }
        return master.getImages().stream().filter(ReleaseUtils::isPrimary).findFirst().orElse(null);
    }


}
