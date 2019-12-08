package org.ruaux.jdiscogs.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.ruaux.jdiscogs.JDiscogsConfiguration;
import org.ruaux.jdiscogs.data.xml.Release;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.redislabs.lettusearch.RediSearchAsyncCommands;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.search.AddOptions;
import com.redislabs.lettusearch.search.Schema;
import com.redislabs.lettusearch.search.api.sync.SearchCommands;
import com.redislabs.lettusearch.search.field.Field;

import io.lettuce.core.RedisFuture;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ReleaseIndexWriter extends ItemStreamSupport implements ItemWriter<Release> {

	public static final String FIELD_TITLE = "title";
	public static final String FIELD_ARTIST = "artist";
	@Autowired
	private JDiscogsConfiguration config;
	@Autowired
	private GenericObjectPool<StatefulRediSearchConnection<String, String>> pool;
	@Autowired
	private StatefulRediSearchConnection<String, String> connection;

	@Override
	public void open(ExecutionContext executionContext) {
		SearchCommands<String, String> commands = connection.sync();
		try {
			commands.drop(config.getData().getReleaseIndex());
		} catch (Exception e) {
			log.debug("Could not drop index {}", config.getData().getReleaseIndex(), e);
		}
		log.info("Creating index {}", config.getData().getReleaseIndex());
		Schema schema = new Schema();
		schema.field(Field.text(FIELD_ARTIST).sortable(true));
		schema.field(Field.text(FIELD_TITLE).sortable(true));
		commands.create(config.getData().getReleaseIndex(), schema);
	}

	@Override
	public void write(List<? extends Release> items) throws Exception {
		log.debug("Writing {} release items", items.size());
		StatefulRediSearchConnection<String, String> connection = pool.borrowObject();
		try {
			RediSearchAsyncCommands<String, String> commands = connection.async();
			commands.setAutoFlushCommands(false);
			List<RedisFuture<?>> futures = new ArrayList<>();
			for (Release release : items) {
				Map<String, String> fields = new HashMap<>();
				if (release.getArtists() != null && !release.getArtists().getArtists().isEmpty()) {
					fields.put(FIELD_ARTIST, release.getArtists().getArtists().get(0).getName());
				}
				fields.put(FIELD_TITLE, release.getTitle());
				futures.add(commands.add(config.getData().getReleaseIndex(), release.getId(), 1, fields,
						new AddOptions().noSave(true)));
			}
			if (config.getData().isNoOp()) {
				return;
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
		} finally {
			pool.returnObject(connection);
		}
	}

}
