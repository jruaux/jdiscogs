package org.ruaux.jdiscogs.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ruaux.jdiscogs.JDiscogsConfiguration;
import org.ruaux.jdiscogs.data.xml.Release;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.search.Document;
import com.redislabs.lettusearch.search.DropOptions;
import com.redislabs.lettusearch.search.Schema;
import com.redislabs.lettusearch.search.api.async.SearchAsyncCommands;
import com.redislabs.lettusearch.search.api.sync.SearchCommands;
import com.redislabs.springredisearch.RediSearchConfiguration;

import io.lettuce.core.RedisException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ReleaseIndexWriter extends ItemStreamSupport implements ItemWriter<Release> {

	public static final String FIELD_TITLE = "title";
	public static final String FIELD_ARTIST = "artist";
	@Autowired
	private JDiscogsConfiguration config;
	@Autowired
	private RediSearchConfiguration rediSearchConfig;
	private StatefulRediSearchConnection<String, String> connection;

	@Override
	public void open(ExecutionContext executionContext) {
		connection = rediSearchConfig.getClient().connect();
		SearchCommands<String, String> commands = connection.sync();
		try {
			commands.drop(config.getData().getReleaseIndex(), DropOptions.builder().build());
		} catch (RedisException e) {
			log.debug("Could not drop index {}", config.getData().getReleaseIndex(), e);
		}
		log.info("Creating index {}", config.getData().getReleaseIndex());
		commands.create(config.getData().getReleaseIndex(),
				Schema.builder().textField(FIELD_ARTIST, true).textField(FIELD_TITLE, true).build());
	}

	@Override
	public void close() {
		if (connection != null) {
			connection.close();
			connection = null;
		}
	}

	@Override
	public void write(List<? extends Release> items) throws Exception {
		SearchAsyncCommands<String, String> commands = connection.async();
		commands.setAutoFlushCommands(false);
		for (Release release : items) {
			Map<String, String> fields = new HashMap<>();
			if (release.getArtists() != null && !release.getArtists().getArtists().isEmpty()) {
				fields.put(FIELD_ARTIST, release.getArtists().getArtists().get(0).getName());
			}
			fields.put(FIELD_TITLE, release.getTitle());
			commands.add(config.getData().getReleaseIndex(),
					Document.builder().id(release.getId()).fields(fields).noSave(true).build());
		}
		commands.flushCommands();
	}

}
