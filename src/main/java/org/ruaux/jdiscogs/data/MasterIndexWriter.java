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
import com.redislabs.lettusearch.RediSearchClient;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.search.AddOptions;
import com.redislabs.lettusearch.search.DropOptions;
import com.redislabs.lettusearch.search.Schema;
import com.redislabs.lettusearch.search.Schema.SchemaBuilder;
import com.redislabs.lettusearch.search.api.sync.SearchCommands;
import com.redislabs.lettusearch.search.field.NumericField;
import com.redislabs.lettusearch.search.field.TextField;
import com.redislabs.lettusearch.suggest.SuggestAddOptions;

import io.lettuce.core.LettuceFutures;
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
	private RediSearchClient client;
	@Autowired
	private JDiscogsConfiguration config;
	private StatefulRediSearchConnection<String, String> connection;

	@Override
	public void open(ExecutionContext executionContext) {
		connection = client.connect();
		SearchCommands<String, String> commands = connection.sync();
		try {
			commands.drop(config.getData().getMasterIndex(), DropOptions.builder().build());
		} catch (Exception e) {
			log.debug("Could not drop index {}", config.getData().getMasterIndex(), e);
		}
		log.info("Creating index {}", config.getData().getMasterIndex());
		SchemaBuilder builder = Schema.builder();
		builder.field(TextField.builder().name(FIELD_ARTIST).sortable(true).build());
		builder.field(TextField.builder().name(FIELD_ARTISTID).sortable(true).build());
		builder.field(TextField.builder().name(FIELD_DATAQUALITY).sortable(true).build());
		builder.field(TextField.builder().name(FIELD_GENRES).sortable(true).build());
		builder.field(TextField.builder().name(FIELD_STYLES).sortable(true).build());
		builder.field(TextField.builder().name(FIELD_TITLE).sortable(true).build());
		builder.field(NumericField.builder().name(FIELD_YEAR).sortable(true).build());
		builder.field(TextField.builder().name(FIELD_IMAGE).sortable(true).build());
		commands.create(config.getData().getMasterIndex(), builder.build());
	}

	@Override
	public synchronized void close() {
		if (connection == null) {
			return;
		}
		connection.close();
		connection = null;
	}

	@Override
	public void write(List<? extends Master> items) throws Exception {
		log.debug("Writing {} master items", items.size());
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
					futures.add(commands.sugadd(config.getData().getArtistSuggestionIndex(), artist.getName(), 1,
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
			futures.add(commands.add(config.getData().getMasterIndex(), master.getId(), 1, fields,
					AddOptions.builder().build()));
		}
		if (config.getData().isNoOp()) {
			return;
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
	}

}
