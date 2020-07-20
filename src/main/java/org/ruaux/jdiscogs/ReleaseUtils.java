package org.ruaux.jdiscogs;

import org.ruaux.jdiscogs.model.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReleaseUtils {

    public static final String LIST_SEPARATOR = " / ";
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

    public static String artist(Track track) {
        if (track.getArtists() == null) {
            return null;
        }
        return track.getArtists().stream().map(Artist::getName).collect(Collectors.joining(LIST_SEPARATOR));
    }

    private static Long duration(Track track) {
        if (track.getDuration() == null) {
            return null;
        }
        Matcher matcher = DURATION_PATTERN.matcher(track.getDuration().trim());
        if (!matcher.matches()) {
            return null;
        }
        int minutes = Integer.parseInt(matcher.group("minutes"));
        int seconds = Integer.parseInt(matcher.group("seconds"));
        return Duration.ofMinutes(minutes).plus(Duration.ofSeconds(seconds)).getSeconds();
    }

    public static List<NormalizedTrack> normalizedTracks(Release release) {
        List<NormalizedTrack> tracks = new ArrayList<>();
        if (release.getTracklist() != null) {
            for (Track track : release.getTracklist()) {
                tracks.add(normalize(track));
                if (track.getSub_tracks() != null) {
                    for (Track subTrack : track.getSub_tracks()) {
                        tracks.add(normalize(subTrack));
                    }
                }
            }
        }
        List<NormalizedTrack> toRemove = new ArrayList<>();
        Integer disc = null;
        int trackNumber = 1;
        for (NormalizedTrack track : tracks) {
            if (track.getPosition() == null || track.getPosition().isEmpty()) {
                Matcher matcher = CD_PATTERN.matcher(track.getTitle());
                if (matcher.matches()) {
                    disc = Integer.parseInt(matcher.group("disc"));
                    trackNumber = 1;
                }
                toRemove.add(track);
            } else {
                Matcher matcher = POSITION_PATTERN.matcher(track.getPosition());
                if (matcher.matches()) {
                    String discString = matcher.group("disc");
                    if (discString != null) {
                        track.setDisc(Integer.parseInt(discString));
                    }
                    String numberString = matcher.group("number");
                    if (numberString != null) {
                        track.setNumber(Integer.parseInt(numberString));
                    }
                    String subString = matcher.group("sub");
                    if (subString != null && !subString.isEmpty()) {
                        NormalizedTrack firstSubTrack = firstSubTrack(tracks, track);
                        if (firstSubTrack != track) {
                            assert firstSubTrack != null;
                            firstSubTrack.setTitle(firstSubTrack.getTitle() + LIST_SEPARATOR + track.getTitle());
                            toRemove.add(track);
                        }
                    }
                }
                if (disc != null) {
                    track.setDisc(disc);
                    track.setNumber(trackNumber);
                }
                trackNumber++;
            }
        }
        tracks.removeAll(toRemove);
        return tracks;
    }

    private static NormalizedTrack normalize(Track track) {
        return NormalizedTrack.builder().artist(artist(track)).duration(duration(track)).title(track.getTitle()).position(track.getPosition()).build();
    }

    public static NormalizedTrack firstSubTrack(List<NormalizedTrack> tracks, NormalizedTrack subTrack) {
        for (NormalizedTrack track : tracks) {
            if (track.getPosition() != null && Objects.equals(track.getDisc(), subTrack.getDisc()) && Objects.equals(track.getNumber(), subTrack.getNumber())) {
                return track;
            }
        }
        return null;
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

    public static Image primaryImage(Release release) {
        return primaryImage(release.getImages());
    }

    private static Image primaryImage(List<Image> images) {
        if (images == null) {
            return null;
        }
        return images.stream().filter(ReleaseUtils::isPrimary).findFirst().orElse(null);
    }

    public static Image primaryImage(Master master) {
        return primaryImage(master.getImages());
    }

}
