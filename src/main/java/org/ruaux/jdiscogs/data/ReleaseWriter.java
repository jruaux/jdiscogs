package org.ruaux.jdiscogs.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.ruaux.jdiscogs.data.xml.Release;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;

import com.redislabs.lettusearch.RediSearchAsyncCommands;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.search.Schema;
import com.redislabs.lettusearch.search.api.SearchCommands;
import com.redislabs.lettusearch.search.field.Field;

import io.lettuce.core.RedisFuture;
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
	private String index;
	private GenericObjectPool<StatefulRediSearchConnection<String, String>> pool;
	private StatefulRediSearchConnection<String, String> connection;

	public ReleaseWriter(JDiscogsBatchProperties props, GenericObjectPool<StatefulRediSearchConnection<String, String>> pool,
			StatefulRediSearchConnection<String, String> connection) {
		this.index = props.getReleaseIndex();
		this.pool = pool;
		this.connection = connection;
	}

	@Override
	public void open(ExecutionContext executionContext) {
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
					fields.put(FIELD_TRACK_NUMBERS,
							Arrays.toString(release.getTrackList().getTracks().stream().map(t -> t.getTrackNumber())
									.filter(n -> n != null).map(n -> n.getTrackNumber()).collect(Collectors.toList()).toArray()));
					fields.put(FIELD_TRACK_DURATIONS,
							Arrays.toString(
									release.getTrackList().getTracks().stream().map(t -> t.getDurationInSeconds())
											.filter(d -> d != null).collect(Collectors.toList()).toArray()));
				}
				futures.add(commands.add(index, release.getId(), 1, fields));
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

}
