package org.ruaux.jdiscogs.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.GenericObjectPool;
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
import com.redislabs.lettusearch.search.Schema.SchemaBuilder;
import com.redislabs.lettusearch.search.api.sync.SearchCommands;
import com.redislabs.lettusearch.search.field.NumericField;
import com.redislabs.lettusearch.search.field.PhoneticMatcher;
import com.redislabs.lettusearch.search.field.TagField;
import com.redislabs.lettusearch.search.field.TextField;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MasterIndexWriter extends ItemStreamSupport implements ItemWriter<Master> {

	public static final String FIELD_ID = "id";
	public static final String FIELD_ARTIST = "artist";
	public static final String FIELD_ARTISTID = "artistId";
	public static final String FIELD_GENRES = "genres";
	public static final String FIELD_TITLE = "title";
	public static final String FIELD_YEAR = "year";

	@Autowired
	private JDiscogsConfiguration config;

	@Autowired
	GenericObjectPool<StatefulRediSearchConnection<String, String>> pool;
	@Autowired
	private StatefulRediSearchConnection<String, String> connection;

	@Override
	public void open(ExecutionContext executionContext) {
		SearchCommands<String, String> commands = connection.sync();
		try {
			commands.drop(config.getData().getMasterIndex(), DropOptions.builder().build());
		} catch (Exception e) {
			log.debug("Could not drop index {}", config.getData().getMasterIndex(), e);
		}
		log.info("Creating index {}", config.getData().getMasterIndex());
		SchemaBuilder builder = Schema.builder();
		builder.field(TextField.builder().name(FIELD_ARTIST).sortable(true).build());
		builder.field(TagField.builder().name(FIELD_ARTISTID).sortable(true).build());
		builder.field(TagField.builder().name(FIELD_GENRES).sortable(true).build());
		builder.field(TextField.builder().name(FIELD_TITLE).matcher(PhoneticMatcher.English).sortable(true).build());
		builder.field(NumericField.builder().name(FIELD_YEAR).sortable(true).build());
		commands.create(config.getData().getMasterIndex(), builder.build());
	}

	@Override
	public void write(List<? extends Master> items) throws Exception {
		log.debug("Writing {} master items", items.size());
		StatefulRediSearchConnection<String, String> connection = pool.borrowObject();
		try {
			RediSearchAsyncCommands<String, String> commands = connection.async();
			commands.setAutoFlushCommands(false);
			List<RedisFuture<?>> futures = new ArrayList<>();
			for (Master master : items) {
				if (master.getImages() == null || master.getImages().getImages() == null
						|| master.getImages().getImages().size() < 4) {
					continue;
				}
//				Image image = master.getImages().getImages().get(0);
//				double ratio = (double) image.getHeight() / image.getWidth();
//				if (ratio < config.getData().getImageRatioMin() || ratio > config.getData().getImageRatioMax()) {
//					continue;
//				}
				if (master.getYear() == null || master.getYear().length() < 4) {
					continue;
				}
				Map<String, String> fields = new HashMap<>();
				if (master.getArtists() != null && !master.getArtists().getArtists().isEmpty()) {
					Artist artist = master.getArtists().getArtists().get(0);
					if (artist != null) {
						fields.put(FIELD_ARTIST, artist.getName());
						fields.put(FIELD_ARTISTID, artist.getId());
						futures.add(commands.sugadd(config.getData().getArtistSuggestionIndex(), artist.getName(), 1,
								true, artist.getId()));
					}
				}
				Set<String> genreSet = new LinkedHashSet<>();
				if (master.getGenres() != null && master.getGenres().getGenres() != null) {
					genreSet.addAll(master.getGenres().getGenres());
				}
				if (master.getStyles() != null && master.getStyles().getStyles() != null) {
					genreSet.addAll(master.getStyles().getStyles());
				}
				fields.put(FIELD_GENRES, String.join(config.getHashArrayDelimiter(), sanitize(genreSet)));
				fields.put(FIELD_TITLE, master.getTitle());
				fields.put(FIELD_YEAR, master.getYear());
				futures.add(commands.add(config.getData().getMasterIndex(), master.getId(), 1, fields,
						AddOptions.builder().build()));
			}
			if (config.getData().isNoOp()) {
				return;
			}
			commands.flushCommands();
			boolean result = LettuceFutures.awaitAll(5, TimeUnit.SECONDS,
					futures.toArray(new RedisFuture[futures.size()]));
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
		} finally

		{
			pool.returnObject(connection);
		}
	}

	private List<String> sanitize(Set<String> genres) {
		List<String> result = new ArrayList<>();
		genres.forEach(genre -> result.add(genre.replace(',', ' ')));
		return result;
	}

}
