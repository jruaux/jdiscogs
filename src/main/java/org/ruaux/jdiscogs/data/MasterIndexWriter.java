package org.ruaux.jdiscogs.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.ruaux.jdiscogs.JDiscogsConfiguration;
import org.ruaux.jdiscogs.data.xml.Artist;
import org.ruaux.jdiscogs.data.xml.Master;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.redislabs.lettusearch.RediSearchAsyncCommands;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.search.AddOptions;
import com.redislabs.lettusearch.search.DropOptions;
import com.redislabs.lettusearch.search.Schema;
import com.redislabs.lettusearch.search.api.sync.SearchCommands;
import com.redislabs.lettusearch.suggest.SuggestAddOptions;
import com.redislabs.springredisearch.RediSearchConfiguration;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisFuture;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MasterIndexWriter extends ItemStreamSupport implements ItemWriter<Master> {

	public static final String FIELD_ARTIST = "artist";
	public static final String FIELD_ARTISTID = "artistId";
	public static final String FIELD_DATAQUALITY = "dataQuality";
	public static final String FIELD_GENRES = "genres";
	public static final String FIELD_STYLES = "styles";
	public static final String FIELD_TITLE = "title";
	public static final String FIELD_YEAR = "year";
	public static final String FIELD_IMAGE = "image";

	@Autowired
	private RediSearchConfiguration searchConfig;
	@Autowired
	private JDiscogsConfiguration config;

	@Override
	public void open(ExecutionContext executionContext) {
		StatefulRediSearchConnection<String, String> connection = searchConfig.getClient().connect();
		SearchCommands<String, String> commands = connection.sync();
		try {
			commands.drop(config.getData().getMasterIndex(), DropOptions.builder().build());
		} catch (RedisException e) {
			log.debug("Could not drop index {}", config.getData().getMasterIndex(), e);
		}
		log.info("Creating index {}", config.getData().getMasterIndex());
		commands.create(config.getData().getMasterIndex(),
				Schema.builder().textField(FIELD_ARTIST, true).textField(FIELD_ARTISTID, true)
						.textField(FIELD_DATAQUALITY, true).textField(FIELD_GENRES, true).textField(FIELD_STYLES, true)
						.textField(FIELD_TITLE, true).numericField(FIELD_YEAR, true).textField(FIELD_IMAGE, true)
						.build());
		connection.close();
	}

	@Override
	public void write(List<? extends Master> items) throws Exception {
		log.debug("Writing {} master items", items.size());
		StatefulRediSearchConnection<String, String> connection = searchConfig.getClient().connect();
		RediSearchAsyncCommands<String, String> commands = connection.async();
		commands.setAutoFlushCommands(false);
		List<RedisFuture<?>> futures = new ArrayList<>();
		for (Master master : items) {
			Map<String, String> fields = new HashMap<>();
			if (master.getArtists() != null && !master.getArtists().getArtists().isEmpty()) {
				Artist artist = master.getArtists().getArtists().get(0);
				if (artist != null) {
					fields.put(FIELD_ARTIST, artist.getName());
					fields.put(FIELD_ARTISTID, artist.getId());
					futures.add(commands.sugadd(config.getData().getArtistSuggestionIndex(), artist.getName(),
							SuggestAddOptions.builder().increment(true).payload(artist.getId()).build()));
				}
			}
			fields.put(FIELD_DATAQUALITY, master.getDataQuality());
			if (master.getGenres() != null) {
				List<String> genres = master.getGenres().getGenres();
				if (genres != null && !genres.isEmpty()) {
					fields.put(FIELD_GENRES, String.join(config.getHashArrayDelimiter(), genres));
				}
			}
			if (master.getStyles() != null) {
				List<String> styles = master.getStyles().getStyles();
				if (styles != null && !styles.isEmpty()) {
					fields.put(FIELD_STYLES, String.join(config.getHashArrayDelimiter(), styles));
				}
			}
			fields.put(FIELD_TITLE, master.getTitle());
			fields.put(FIELD_YEAR, master.getYear());
			Boolean image = master.getImages() != null && !master.getImages().getImages().isEmpty();
			fields.put(FIELD_IMAGE, image.toString());
			futures.add(commands.add(config.getData().getMasterIndex(), master.getId(), fields, 1d,
					AddOptions.builder().build()));
		}
		commands.flushCommands();
		boolean result = LettuceFutures.awaitAll(5, TimeUnit.SECONDS, futures.toArray(new RedisFuture[futures.size()]));
		if (result) {
			log.debug("Wrote {} master items", items.size());
		} else {
			log.warn("Could not write {} master items", items.size());
			for (RedisFuture<?> future : futures) {
				if (future.getError() != null) {
					log.error(future.getError());
				}
			}
		}
		connection.close();
	}

}
