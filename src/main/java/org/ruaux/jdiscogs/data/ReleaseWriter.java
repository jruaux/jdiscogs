package org.ruaux.jdiscogs.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.ruaux.jdiscogs.data.xml.Release;
import org.ruaux.jdiscogs.data.xml.Track;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;

import com.redislabs.lettusearch.RediSearchAsyncCommands;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.search.Schema;
import com.redislabs.lettusearch.search.api.SearchCommands;
import com.redislabs.lettusearch.search.field.Field;

import io.lettuce.core.RedisFuture;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReleaseWriter extends ItemStreamSupport implements ItemWriter<Release> {

	public static final String FIELD_TITLE = "title";
	public static final String FIELD_ARTIST = "artist";
	public static final String FIELD_FORMAT = "format";
	public static final String FIELD_MASTER = "master";
	public static final String FIELD_MAIN_RELEASE = "mainRelease";
	public static final String FIELD_TRACKS = "tracks";
	public static final String FIELD_TRACK_NUMBERS = "trackNumbers";
	public static final String FIELD_TRACK_DURATIONS = "trackDurations";
	private JDiscogsBatchProperties props;
	private GenericObjectPool<StatefulRediSearchConnection<String, String>> pool;
	private StatefulRediSearchConnection<String, String> connection;

	public ReleaseWriter(JDiscogsBatchProperties props,
			GenericObjectPool<StatefulRediSearchConnection<String, String>> pool,
			StatefulRediSearchConnection<String, String> connection) {
		this.props = props;
		this.pool = pool;
		this.connection = connection;
	}

	@Override
	public void open(ExecutionContext executionContext) {
		String index = props.getReleaseIndex();
		SearchCommands<String, String> commands = connection.sync();
		try {
			commands.drop(index);
		} catch (Exception e) {
			log.debug("Could not drop index {}", index, e);
		}
		log.info("Creating index {}", index);
		commands.create(index, Schema.builder().field(Field.text(FIELD_ARTIST).sortable(true))
				.field(Field.text(FIELD_TITLE).sortable(true)).build());
	}

	@Override
	public void write(List<? extends Release> items) throws Exception {
		log.debug("Writing {} release items", items.size());
		try (StatefulRediSearchConnection<String, String> connection = pool.borrowObject()) {
			RediSearchAsyncCommands<String, String> commands = connection.async();
			commands.setAutoFlushCommands(false);
			List<RedisFuture<?>> futures = new ArrayList<>();
			for (Release release : items) {
				Map<String, String> fields = new HashMap<>();
				if (release.getArtists() != null && !release.getArtists().getArtists().isEmpty()) {
					fields.put(FIELD_ARTIST, release.getArtists().getArtists().get(0).getName());
				}
				fields.put(FIELD_TITLE, release.getTitle());
				if (release.getFormats() != null && !release.getFormats().getFormats().isEmpty()) {
					fields.put(FIELD_FORMAT, release.getFormats().getFormats().get(0).getName());
				}
				if (release.getMasterId() != null) {
					fields.put(FIELD_MASTER, release.getMasterId().getMasterId());
					fields.put(FIELD_MAIN_RELEASE,
							String.valueOf(Boolean.TRUE.equals(release.getMasterId().getMainRelease())));
				}
				if (release.getTrackList() != null && !release.getTrackList().getTracks().isEmpty()) {
					fields.put(FIELD_TRACKS, String.valueOf(release.getTrackList().getTracks().size()));
					String positions = String.join(props.getHashArrayDelimiter(), release.getTrackList().getTracks()
							.stream().map(t -> t.getPosition()).collect(Collectors.toList()));
					fields.put(FIELD_TRACK_NUMBERS, positions);
					String durations = String.join(props.getHashArrayDelimiter(), release.getTrackList().getTracks().stream()
									.map(t -> t.getDuration()).collect(Collectors.toList()));
					fields.put(FIELD_TRACK_DURATIONS, durations);
				}
				futures.add(commands.add(props.getReleaseIndex(), release.getId(), 1, fields));
			}
			commands.flushCommands();
			for (int index = 0; index < futures.size(); index++) {
				RedisFuture<?> future = futures.get(index);
				if (future == null) {
					continue;
				}
				try {
					future.get(1, TimeUnit.SECONDS);
				} catch (Exception e) {
					log.error("Could not write record {}", items.get(index), e);
				}
			}
		}
	}

	private final static Pattern cdTrackPattern = Pattern.compile("^((?<disc>\\d+)[\\.\\-])?(?<track>\\d+)");
	private final static Pattern vinylTrackPattern = Pattern.compile("^(?<side>[a-zΑ-Ω\\wа-яА-Я])(?<track>\\d+)?",
			Pattern.CASE_INSENSITIVE);
	private final static Pattern durationPattern = Pattern.compile("^(?<minutes>\\d+)\\:(?<seconds>\\d+)");

	@Builder
	public static @Data class TrackNumber {
		private Integer discNumber;
		private Integer trackNumber;
		private String side;
	}

	public TrackNumber getTrackNumber(Track track) {
		String position = track.getPosition();
		if (position != null && !position.isBlank()) {
			Matcher cdMatcher = cdTrackPattern.matcher(position.trim());
			if (cdMatcher.find()) {
				String disc = cdMatcher.group("disc");
				try {
					int trackNumber = Integer.parseInt(cdMatcher.group("track"));
					Integer discNumber = disc == null ? null : Integer.parseInt(disc);
					return TrackNumber.builder().discNumber(discNumber).trackNumber(trackNumber).build();
				} catch (NumberFormatException e) {
					log.error("Could not parse cd track position {}", position);
				}
			} else {
				Matcher vinylMatcher = vinylTrackPattern.matcher(position.trim());
				if (vinylMatcher.find()) {
					String trackNumberString = vinylMatcher.group("track");
					try {
						Integer trackNumber = trackNumberString == null ? null : Integer.parseInt(trackNumberString);
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

	public Integer getDurationInSeconds(Track track) {
		Matcher matcher = durationPattern.matcher(track.getDuration().trim());
		if (matcher.matches()) {
			try {
				return Integer.parseInt(matcher.group("minutes")) * 60 + Integer.parseInt(matcher.group("seconds"));
			} catch (NumberFormatException e) {
				log.error("Could not parse duration ''", track.getDuration());
			}
		}
		return null;
	}

}
