package org.ruaux.jdiscogs.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	public final static String ARTIST = "artist";
	public final static String TITLE = "title";
	public final static String ID = "id";
	public final static String XML = "xml";

	private JDiscogsBatchProperties props;
	private GenericObjectPool<StatefulRediSearchConnection<String, String>> pool;
	private TextSanitizer sanitizer = new TextSanitizer();
	private ReleaseCodec codec;

	public ReleaseWriter(JDiscogsBatchProperties props,
			GenericObjectPool<StatefulRediSearchConnection<String, String>> pool, ReleaseCodec codec) {
		this.props = props;
		this.pool = pool;
		this.codec = codec;
	}

	@Override
	public synchronized void open(ExecutionContext executionContext) {
		String index = props.getReleaseIndex();
		try (StatefulRediSearchConnection<String, String> connection = pool.borrowObject()) {
			SearchCommands<String, String> commands = connection.sync();
			try {
				commands.drop(index);
			} catch (Exception e) {
				log.debug("Could not drop index {}", index, e);
			}
			log.info("Creating index {}", index);
			commands.create(index, Schema.builder().field(Field.text(ARTIST).sortable(true))
					.field(Field.tag(ID).sortable(true)).field(Field.text(TITLE).sortable(true)).build());
		} catch (Exception e) {
			log.error("Could not create index {}", index, e);
		}
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
				Stream<String> artists = release.getArtists().getArtists().stream().map(a -> a.getName());
				fields.put(ARTIST, sanitizer.sanitize(String.join(" ", artists.collect(Collectors.toList()))));
				fields.put(TITLE, sanitizer.sanitize(release.getTitle()));
				fields.put(ID, release.getId());
				fields.put(XML, codec.xml(release));
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
