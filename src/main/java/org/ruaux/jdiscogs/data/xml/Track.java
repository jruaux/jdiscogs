package org.ruaux.jdiscogs.data.xml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@XmlRootElement(name = "track")
@XmlAccessorType(XmlAccessType.FIELD)
public @Data class Track {

	private final static Pattern cdTrackPattern = Pattern.compile("^((?<disc>\\d+)[\\.\\-])?(?<track>\\d+)");
	private final static Pattern vinylTrackPattern = Pattern.compile("^(?<side>[a-zΑ-Ω\\wа-яА-Я])(?<track>\\d+)?",
			Pattern.CASE_INSENSITIVE);
	private final static Pattern durationPattern = Pattern.compile("^(?<minutes>\\d+)\\:(?<seconds>\\d+)");

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

	@Builder
	public static @Data class TrackNumber {
		private Integer discNumber;
		private Integer trackNumber;
		private String side;
	}

	public TrackNumber getTrackNumber() {
		if (position != null && !position.isBlank()) {
			Matcher cdMatcher = cdTrackPattern.matcher(position.trim());
			if (cdMatcher.find()) {
				String disc = cdMatcher.group("disc");
				try {
					int track = Integer.parseInt(cdMatcher.group("track"));
					Integer discNumber = disc == null ? null : Integer.parseInt(disc);
					return TrackNumber.builder().discNumber(discNumber).trackNumber(track).build();
				} catch (NumberFormatException e) {
					log.error("Could not parse cd track position {}", position);
				}
			} else {
				Matcher vinylMatcher = vinylTrackPattern.matcher(position.trim());
				if (vinylMatcher.find()) {
					String track = vinylMatcher.group("track");
					try {
						Integer trackNumber = track == null ? null : Integer.parseInt(track);
						return TrackNumber.builder().side(vinylMatcher.group("side")).trackNumber(trackNumber).build();
					} catch (NumberFormatException e) {
						log.error("Could not parse vinyl track position {}", position);
					}
				} else {
					log.error("Could not match track position to CD or Vinyl formats '{}'", position);
				}
			}
		}
		return null;
	}

	public Integer getDurationInSeconds() {
		Matcher matcher = durationPattern.matcher(duration.trim());
		if (matcher.matches()) {
			try {
				return Integer.parseInt(matcher.group("minutes")) * 60 + Integer.parseInt(matcher.group("seconds"));
			} catch (NumberFormatException e) {
				log.error("Could not parse duration ''", duration);
			}
		}
		return null;
	}

	public static void main(String[] args) {
		Track track = new Track();
		track.setPosition("Α3");
		System.out.println(track.getTrackNumber());
	}
}
