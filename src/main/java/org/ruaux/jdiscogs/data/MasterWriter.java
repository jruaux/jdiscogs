package org.ruaux.jdiscogs.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.ruaux.jdiscogs.data.xml.Artist;
import org.ruaux.jdiscogs.data.xml.Image;
import org.ruaux.jdiscogs.data.xml.Master;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;

import com.redislabs.lettusearch.RediSearchAsyncCommands;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.search.Schema;
import com.redislabs.lettusearch.search.api.SearchCommands;
import com.redislabs.lettusearch.search.field.Field;
import com.redislabs.lettusearch.search.field.PhoneticMatcher;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MasterWriter extends ItemStreamSupport implements ItemWriter<Master> {

	public static final String ARTIST = "artist";
	public static final String ARTIST_ID = "artistId";
	public static final String GENRES = "getGenres";
	public static final String TITLE = "title";
	public static final String YEAR = "year";

	private JDiscogsBatchProperties props;
	private GenericObjectPool<StatefulRediSearchConnection<String, String>> pool;

	public MasterWriter(JDiscogsBatchProperties props,
			GenericObjectPool<StatefulRediSearchConnection<String, String>> pool) {
		this.props = props;
		this.pool = pool;
	}

	@Override
	public void open(ExecutionContext executionContext) {
		String index = props.getMasterIndex();
		try (StatefulRediSearchConnection<String, String> connection = pool.borrowObject()) {
			SearchCommands<String, String> commands = connection.sync();
			try {
				commands.drop(index);
			} catch (Exception e) {
				log.debug("Could not drop index {}", index, e);
			}
			log.info("Creating index {}", index);
			Schema schema = Schema.builder().field(Field.text(ARTIST).sortable(true))
					.field(Field.tag(ARTIST_ID).sortable(true)).field(Field.tag(GENRES).sortable(true))
					.field(Field.text(TITLE).matcher(PhoneticMatcher.English).sortable(true))
					.field(Field.numeric(YEAR).sortable(true)).build();
			commands.create(index, schema);
		} catch (Exception e) {
			log.error("Could not create index {}", index, e);
		}
	}

	@Override
	public void write(List<? extends Master> items) throws Exception {
		log.debug("Writing {} master items", items.size());
		try (StatefulRediSearchConnection<String, String> connection = pool.borrowObject()) {
			RediSearchAsyncCommands<String, String> commands = connection.async();
			commands.setAutoFlushCommands(false);
			List<RedisFuture<?>> futures = new ArrayList<>();
			for (Master master : items) {
				if (master.getImages() == null || master.getImages().getImages() == null) {
					continue;
				}
				if (master.getImages().getImages().size() < props.getMinImages()) {
					continue;
				}
				Image image = master.getPrimaryImage();
				if (image == null) {
					continue;
				}
				if (!withinRange(image.getHeight(), props.getImageHeight())) {
					continue;
				}
				if (!withinRange(image.getWidth(), props.getImageWidth())) {
					continue;
				}
				if (!withinRange(image.getRatio(), props.getImageRatio())) {
					continue;
				}
				if (master.getYear() == null || master.getYear().length() < 4) {
					continue;
				}
				Map<String, String> fields = new HashMap<>();
				if (master.getArtists() != null && !master.getArtists().getArtists().isEmpty()) {
					Artist artist = master.getArtists().getArtists().get(0);
					if (artist != null) {
						fields.put(ARTIST, artist.getName());
						fields.put(ARTIST_ID, artist.getId());
						futures.add(commands.sugadd(props.getArtistSuggestionIndex(), artist.getName(), 1, true,
								artist.getId()));
					}
				}
				Set<String> genres = new LinkedHashSet<>();
				if (master.getGenres() != null && master.getGenres().getGenres() != null) {
					genres.addAll(master.getGenres().getGenres());
				}
				if (master.getStyles() != null && master.getStyles().getStyles() != null) {
					genres.addAll(master.getStyles().getStyles());
				}
				fields.put(GENRES, String.join(props.getHashArrayDelimiter(), sanitize(genres)));
				fields.put(TITLE, master.getTitle());
				fields.put(YEAR, master.getYear());
				futures.add(commands.add(props.getMasterIndex(), master.getId(), 1, fields));
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
		}
	}

	private boolean withinRange(Number value, Range range) {
		if (value == null) {
			return false;
		}
		return value.doubleValue() >= range.getMin() && value.doubleValue() <= range.getMax();
	}

	private List<String> sanitize(Set<String> getGenres) {
		List<String> result = new ArrayList<>();
		getGenres.forEach(genre -> result.add(genre.replace(',', ' ')));
		return result;
	}

}
