package org.ruaux.jdiscogs.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.ruaux.jdiscogs.JDiscogsProperties;
import org.ruaux.jdiscogs.Range;
import org.ruaux.jdiscogs.data.xml.Artist;
import org.ruaux.jdiscogs.data.xml.Image;
import org.ruaux.jdiscogs.data.xml.Master;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.redislabs.lettusearch.RediSearchAsyncCommands;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.search.Schema;
import com.redislabs.lettusearch.search.api.SearchCommands;
import com.redislabs.lettusearch.search.field.Field;
import com.redislabs.lettusearch.search.field.PhoneticMatcher;

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

	private JDiscogsProperties props;
	private GenericObjectPool<StatefulRediSearchConnection<String, String>> pool;
	private StatefulRediSearchConnection<String, String> connection;

	public MasterIndexWriter(JDiscogsProperties props,
			GenericObjectPool<StatefulRediSearchConnection<String, String>> pool,
			StatefulRediSearchConnection<String, String> connection) {
		this.props = props;
		this.pool = pool;
		this.connection = connection;
	}

	@Override
	public void open(ExecutionContext executionContext) {
		SearchCommands<String, String> commands = connection.sync();
		try {
			commands.drop(props.masterIndex());
		} catch (Exception e) {
			log.debug("Could not drop index {}", props.masterIndex(), e);
		}
		log.info("Creating index {}", props.masterIndex());
		Schema schema = Schema.builder().field(Field.text(FIELD_ARTIST).sortable(true))
				.field(Field.tag(FIELD_ARTISTID).sortable(true)).field(Field.tag(FIELD_GENRES).sortable(true))
				.field(Field.text(FIELD_TITLE).matcher(PhoneticMatcher.English).sortable(true))
				.field(Field.numeric(FIELD_YEAR).sortable(true)).build();
		commands.create(props.masterIndex(), schema);
	}

	@Override
	public void write(List<? extends Master> items) throws Exception {
		log.debug("Writing {} master items", items.size());
		try (StatefulRediSearchConnection<String, String> connection = pool.borrowObject()) {
			RediSearchAsyncCommands<String, String> commands = connection.async();
			commands.setAutoFlushCommands(false);
			List<RedisFuture<?>> futures = new ArrayList<>();
			for (Master master : items) {
				if (master.images() == null || master.images().images() == null) {
					continue;
				}
				if (master.images().images().size() < props.minImages()) {
					continue;
				}
				Image image = master.getPrimaryImage();
				if (image == null) {
					continue;
				}
				if (!withinRange(image.height(), props.imageHeight())) {
					continue;
				}
				if (!withinRange(image.width(), props.imageWidth())) {
					continue;
				}
				if (!withinRange(image.getRatio(), props.imageRatio())) {
					continue;
				}
				if (master.year() == null || master.year().length() < 4) {
					continue;
				}
				Map<String, String> fields = new HashMap<>();
				if (master.artists() != null && !master.artists().artists().isEmpty()) {
					Artist artist = master.artists().artists().get(0);
					if (artist != null) {
						fields.put(FIELD_ARTIST, artist.name());
						fields.put(FIELD_ARTISTID, artist.id());
						futures.add(
								commands.sugadd(props.artistSuggestionIndex(), artist.name(), 1, true, artist.id()));
					}
				}
				Set<String> genreSet = new LinkedHashSet<>();
				if (master.genres() != null && master.genres().genres() != null) {
					genreSet.addAll(master.genres().genres());
				}
				if (master.styles() != null && master.styles().styles() != null) {
					genreSet.addAll(master.styles().styles());
				}
				fields.put(FIELD_GENRES, String.join(props.hashArrayDelimiter(), sanitize(genreSet)));
				fields.put(FIELD_TITLE, master.title());
				fields.put(FIELD_YEAR, master.year());
				futures.add(commands.add(props.masterIndex(), master.id(), 1, fields));
			}
			if (props.noOp()) {
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
		}
	}

	private boolean withinRange(Number value, Range range) {
		if (value == null) {
			return false;
		}
		return value.doubleValue() >= range.min() && value.doubleValue() <= range.max();
	}

	private List<String> sanitize(Set<String> genres) {
		List<String> result = new ArrayList<>();
		genres.forEach(genre -> result.add(genre.replace(',', ' ')));
		return result;
	}

}
