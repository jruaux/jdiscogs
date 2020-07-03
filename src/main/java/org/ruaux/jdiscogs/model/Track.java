package org.ruaux.jdiscogs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.annotation.*;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "track")
@XmlAccessorType(XmlAccessType.FIELD)
public class Track implements Comparable<Track> {

    private static final Pattern POSITION_PATTERN = Pattern.compile("^((?<side>[A-Z])|((?<disc>\\d+)[\\-.:]))?(?<number>\\d+)(?<sub>[a-z])?");
    private static final Pattern DURATION_PATTERN = Pattern.compile("^(?<minutes>\\d+):(?<seconds>\\d+)");

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

    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position implements Comparable<Position> {
        private Integer number;
        private Integer disc;
        private Character side;
        private Character sub;

        @Override
        public String toString() {
            String result = "";
            if (side != null) {
                result += side;
            }
            if (disc != null) {
                result += disc + "-";
            }
            if (number != null) {
                result += number;
            }
            if (sub != null) {
                result += sub;
            }
            return result;
        }


        @Override
        public int compareTo(Position position) {
            if (disc == null) {
                if (position.disc == null) {
                    if (side == null) {
                        if (position.side == null) {
                            return compareTrackNumbers(position);
                        }
                        return -1;
                    }
                    if (position.side == null) {
                        return 1;
                    }
                    int c = Integer.compare(side, position.side);
                    if (c == 0) {
                        return compareTrackNumbers(position);
                    }
                    return c;
                }
                return -1;
            }
            if (position.disc == null) {
                return 1;
            }
            int c = Integer.compare(disc, position.disc);
            if (c == 0) {
                return compareTrackNumbers(position);
            }
            return c;
        }


        private int compareTrackNumbers(Position position) {
            if (number == null) {
                if (position.number == null) {
                    return 0;
                }
                return -1;
            }
            if (position.number == null) {
                return 1;
            }
            int c = Integer.compare(number, position.number);
            if (c == 0) {
                if (sub == null) {
                    if (position.sub == null) {
                        return 0;
                    }
                    return -1;
                }
                if (position.sub == null) {
                    return 1;
                }
                return Character.compare(sub, position.sub);
            }
            return c;
        }
    }

    public Position position() {
        if (position != null) {
            Matcher matcher = POSITION_PATTERN.matcher(position);
            if (matcher.matches()) {
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
        }
        return null;
    }

    @Override
    public int compareTo(Track track) {
        Position p1 = position();
        Position p2 = track.position();
        if (p1 == null) {
            if (p2 == null) {
                return 0;
            }
            return -1;
        }
        if (p2 == null) {
            return 1;
        }
        return p1.compareTo(p2);
    }

    public Duration duration() {
        if (duration != null) {
            Matcher matcher = DURATION_PATTERN.matcher(duration.trim());
            if (matcher.matches()) {
                int minutes = Integer.parseInt(matcher.group("minutes"));
                int seconds = Integer.parseInt(matcher.group("seconds"));
                Duration duration = Duration.ofMinutes(minutes);
                return duration.plus(Duration.ofSeconds(seconds));
            }
        }
        return null;
    }


}