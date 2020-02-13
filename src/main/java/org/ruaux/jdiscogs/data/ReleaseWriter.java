package org.ruaux.jdiscogs.data;

import static org.ruaux.jdiscogs.data.Fields.ARTIST;
import static org.ruaux.jdiscogs.data.Fields.FORMAT;
import static org.ruaux.jdiscogs.data.Fields.MAIN_RELEASE;
import static org.ruaux.jdiscogs.data.Fields.MASTER;
import static org.ruaux.jdiscogs.data.Fields.TITLE;
import static org.ruaux.jdiscogs.data.Fields.TRACKS;
import static org.ruaux.jdiscogs.data.Fields.TRACK_ARTIST;
import static org.ruaux.jdiscogs.data.Fields.TRACK_DURATION;
import static org.ruaux.jdiscogs.data.Fields.TRACK_POSITION;
import static org.ruaux.jdiscogs.data.Fields.TRACK_TITLE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReleaseWriter extends ItemStreamSupport implements ItemWriter<Release> {

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
		commands.create(index, Schema.builder().field(Field.text(ARTIST).sortable(true))
				.field(Field.text(TITLE).sortable(true)).build());
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
					fields.put(ARTIST, release.getArtists().getArtists().get(0).getName());
				}
				fields.put(TITLE, release.getTitle());
				if (release.getFormats() != null && !release.getFormats().getFormats().isEmpty()) {
					fields.put(FORMAT, release.getFormats().getFormats().get(0).getName());
				}
				if (release.getMasterId() != null) {
					fields.put(MASTER, release.getMasterId().getMasterId());
					fields.put(MAIN_RELEASE,
							String.valueOf(Boolean.TRUE.equals(release.getMasterId().getMainRelease())));
				}
				if (release.getTrackList() != null && !release.getTrackList().getTracks().isEmpty()) {
					List<Track> tracks = release.getTrackList().getTracks();
					fields.put(TRACKS, String.valueOf(tracks.size()));
					for (int index = 0; index < tracks.size(); index++) {
						String suffix = "[" + index + "]";
						Track track = tracks.get(index);
						fields.put(TRACK_TITLE + suffix, track.getTitle());
						fields.put(TRACK_POSITION + suffix, track.getPosition());
						fields.put(TRACK_DURATION + suffix, track.getDuration());
						if (track.getArtists() != null && !track.getArtists().getArtists().isEmpty()) {
							fields.put(TRACK_ARTIST + suffix, track.getArtists().getArtists().get(0).getName());
						}
					}
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

}
