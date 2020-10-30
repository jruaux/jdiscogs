package org.ruaux.jdiscogs.data;

import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ruaux.jdiscogs.data.NormalizedRelease.Format;
import org.ruaux.jdiscogs.model.Artist;
import org.ruaux.jdiscogs.model.Release;
import org.ruaux.jdiscogs.model.Track;

public class Helper {

	public static final String UNKNOWN = "Unknown";
	public static final String VARIOUS = "Various";
	public static final String ARRAY_SEPARATOR = ",";

	private static final String DISCOGS_RELEASE_URL = "https://www.discogs.com/release/";
	private static final Pattern RELEASED_PATTERN = Pattern.compile("^(?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})");
	private static final String CD = "cd";
	private static final String VINYL = "vinyl";
	private static final String ENTITY_NUMBER_REPLACE = "\\s+\\(\\d+\\)";
	private static final Pattern POSITION_PATTERN = Pattern
			.compile("^((?<side>[A-Z])|((?<disc>\\d+)[\\-.:]))?(?<number>\\d+)(?<sub>[a-z])?");
	private static final Pattern CD_PATTERN = Pattern.compile("(disc|disk|cd)\\s*(?<disc>\\d+).*",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern DURATION_PATTERN = Pattern.compile("^(?<minutes>\\d+):(?<seconds>\\d+)");

	public static String getUri(Release release) {
		return DISCOGS_RELEASE_URL + release.getId();
	}

	private static long duration(Track track) {
		if (track.getDuration() == null) {
			return 0;
		}
		Matcher matcher = DURATION_PATTERN.matcher(track.getDuration().trim());
		if (matcher.matches()) {
			int minutes = Integer.parseInt(matcher.group("minutes"));
			int seconds = Integer.parseInt(matcher.group("seconds"));
			return Duration.ofMinutes(minutes).plus(Duration.ofSeconds(seconds)).getSeconds();
		}
		return 0;
	}

	private static String removeEntityNumber(String name) {
		return name.replaceAll(ENTITY_NUMBER_REPLACE, "").trim();
	}

	public static NormalizedRelease normalize(Release release) {
		List<NormalizedTrack> tracks = tracks(release).stream()
				.map(t -> NormalizedTrack.builder().title(t.getTitle()).duration(duration(t)).build())
				.collect(Collectors.toList());
		return NormalizedRelease.builder().id(release.getId()).artist(artist(release)).title(release.getTitle())
				.format(format(release)).tracks(tracks).build();
	}

	private static List<Track> tracks(Release release) {
		List<Track> tracks = new ArrayList<>();
		if (release.getTracklist() != null) {
			for (Track track : release.getTracklist()) {
				tracks.add(track);
				if (track.getSub_tracks() != null) {
					for (Track subTrack : track.getSub_tracks()) {
						tracks.add(subTrack);
					}
				}
			}
		}
		List<Track> toRemove = new ArrayList<>();
		Integer disc = null;
		int trackNumber = 1;
		Map<Track, Integer> discs = new HashMap<>();
		Map<Track, Integer> numbers = new HashMap<>();
		for (Track track : tracks) {
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
					String side = matcher.group("side");
					if (side == null) {
						String discString = matcher.group("disc");
						if (discString != null) {
							discs.put(track, Integer.parseInt(discString));
						}
						String numberString = matcher.group("number");
						if (numberString != null) {
							numbers.put(track, Integer.parseInt(numberString));
						}
					} else {
						numbers.put(track, trackNumber);
					}
					String subString = matcher.group("sub");
					if (subString != null && !subString.isEmpty()) {
						Track firstSubTrack = firstSubTrack(tracks, track, discs, numbers);
						if (firstSubTrack != track) {
							assert firstSubTrack != null;
							firstSubTrack.setTitle(firstSubTrack.getTitle() + " - " + track.getTitle());
							toRemove.add(track);
						}
					}
				}
				if (disc != null) {
					discs.put(track, disc);
					numbers.put(track, trackNumber);
				}
				trackNumber++;
			}
		}
		tracks.removeAll(toRemove);
		return tracks;
	}

	private static Track firstSubTrack(List<Track> tracks, Track subTrack, Map<Track, Integer> discs,
			Map<Track, Integer> numbers) {
		for (Track track : tracks) {
			if (track.getPosition() != null && Objects.equals(discs.get(track), discs.get(subTrack))
					&& Objects.equals(numbers.get(track), numbers.get(subTrack))) {
				return track;
			}
		}
		return null;
	}

	private static String artist(Release release) {
		return artist(release.getArtists());
	}

	private static String artist(List<Artist> artists) {
		if (artists == null) {
			return null;
		}
		return join(artists.stream().map(a -> removeEntityNumber(a.getName())));
	}

	public static String join(Stream<String> stream) {
		return stream.collect(Collectors.joining(" - "));
	}

	private static Format format(Release release) {
		if (release.getFormats() == null || release.getFormats().isEmpty()) {
			return Format.NONE;
		}
		switch (release.getFormats().get(0).getName().toLowerCase()) {
		case CD:
			return Format.CD;
		case VINYL:
			return Format.VINYL;
		default:
			return Format.OTHER;
		}
	}

	public static String firstValue(List<String> collection) {
		if (collection == null || collection.isEmpty()) {
			return null;
		}
		return collection.get(0);
	}

	public static Integer getYear(Release release) {
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

	public static String sanitize(String string) {
		return Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
	}

}
