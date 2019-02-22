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

import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.search.AddOptions;
import com.redislabs.lettusearch.search.DropOptions;
import com.redislabs.lettusearch.search.Schema;
import com.redislabs.lettusearch.search.Schema.SchemaBuilder;
import com.redislabs.lettusearch.search.api.async.SearchAsyncCommands;
import com.redislabs.lettusearch.search.api.sync.SearchCommands;
import com.redislabs.lettusearch.search.field.TextField;

import io.lettuce.core.LettuceFutures;
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
			commands.drop(config.getData().getReleaseIndex(), DropOptions.builder().build());
		} catch (Exception e) {
			log.debug("Could not drop index {}", config.getData().getReleaseIndex(), e);
		}
		log.info("Creating index {}", config.getData().getReleaseIndex());
		SchemaBuilder builder = Schema.builder();
		builder.field(TextField.builder().name(FIELD_ARTIST).sortable(true).build());
		builder.field(TextField.builder().name(FIELD_TITLE).sortable(true).build());
		commands.create(config.getData().getReleaseIndex(), builder.build());
	}

	@Override
	public void write(List<? extends Release> items) throws Exception {
		log.debug("Writing {} release items", items.size());
		StatefulRediSearchConnection<String, String> connection = pool.borrowObject();
		try {
			SearchAsyncCommands<String, String> commands = connection.async();
			commands.setAutoFlushCommands(false);
			List<RedisFuture<?>> futures = new ArrayList<>();
			for (Release release : items) {
				Map<String, String> fields = new HashMap<>();
				if (release.getArtists() != null && !release.getArtists().getArtists().isEmpty()) {
					fields.put(FIELD_ARTIST, release.getArtists().getArtists().get(0).getName());
				}
				fields.put(FIELD_TITLE, release.getTitle());
				futures.add(commands.add(config.getData().getReleaseIndex(), release.getId(), 1, fields,
						AddOptions.builder().noSave(true).build()));
			}
			if (config.getData().isNoOp()) {
				return;
			}
			commands.flushCommands();
			boolean result = LettuceFutures.awaitAll(5, TimeUnit.SECONDS,
					futures.toArray(new RedisFuture[futures.size()]));
			if (result) {
				log.debug("Wrote {} release items", items.size());
			} else {
				log.warn("Could not write {} release items", items.size());
				for (RedisFuture<?> future : futures) {
					if (future.getError() != null) {
						log.error(future.getError());
					}
				}
			}
		} finally {
			pool.returnObject(connection);
		}
	}

}
