package org.ruaux.jdiscogs.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Track {

	private static final Pattern DURATION_PATTERN = Pattern.compile("^(?<minutes>\\d+):(?<seconds>\\d+)");
	private static final Pattern POSITION_PATTERN = Pattern.compile("^((?<side>[A-Z])|((?<disc>\\d+)[\\-.:]))?(?<number>\\d+)(?<sub>[a-z])?\\.?$");

	@Getter
	@Setter
	private String duration;
	@Getter
	@Setter
	private String position;
	@Getter
	@Setter
	private String type_;
	@Getter
	@Setter
	private String title;
	@Getter
	@Setter
	private List<Artist> extraartists;
	@Getter
	@Setter
	private List<Artist> artists;
	@Setter
	private List<Track> sub_tracks;

	public List<Track> getSub_tracks() {
		if (sub_tracks==null || sub_tracks.isEmpty()) {
			return Collections.emptyList();
		}
		if (getNumber() != null && (getSubPosition() == null || "a".equalsIgnoreCase(getSubPosition()))) {
			return Collections.emptyList();
		}
		return sub_tracks;
	}

	public String getArtist() {
		if (artists==null || artists.isEmpty()) {
			return null;
		}
		return artists.get(0).getName();
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