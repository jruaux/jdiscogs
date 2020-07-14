package org.ruaux.jdiscogs.model;

import lombok.Data;

@Data
public class Position implements Comparable<Position> {

    private String string;
    private Integer number;
    private Integer disc;
    private Character side;
    private Character sub;

    @Override
    public String toString() {
        return string;
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