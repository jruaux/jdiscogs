package org.ruaux.jdiscogs.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ruaux.jdiscogs.JDiscogsConfiguration;
import org.ruaux.jdiscogs.data.xml.Artist;
import org.ruaux.jdiscogs.data.xml.Master;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.redislabs.lettusearch.RediSearchClient;
import com.redislabs.lettusearch.api.Document;
import com.redislabs.lettusearch.api.DropOptions;
import com.redislabs.lettusearch.api.Schema;
import com.redislabs.lettusearch.api.Suggestion;
import com.redislabs.lettusearch.api.async.SearchAsyncCommands;
import com.redislabs.lettusearch.api.sync.SearchCommands;
import com.redislabs.springredisearch.RediSearchConfiguration;

import io.lettuce.core.RedisException;
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
	private RediSearchClient client;

	@Override
	public void open(ExecutionContext executionContext) {
		client = searchConfig.getClient();
		SearchCommands<String, String> commands = client.connect().sync();
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
	}

	@Override
	public void write(List<? extends Master> items) throws Exception {
		log.debug("Writing {} master items", items.size());
		SearchAsyncCommands<String, String> commands = client.connect().async();
		commands.setAutoFlushCommands(false);
		for (Master master : items) {
			Map<String, String> fields = new HashMap<>();
			if (master.getArtists() != null && !master.getArtists().getArtists().isEmpty()) {
				Artist artist = master.getArtists().getArtists().get(0);
				if (artist != null) {
					fields.put(FIELD_ARTIST, artist.getName());
					fields.put(FIELD_ARTISTID, artist.getId());
					commands.add(config.getData().getArtistSuggestionIndex(), Suggestion.builder()
							.string(artist.getName()).increment(true).payload(artist.getId()).build());
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
			commands.add(config.getData().getMasterIndex(),
					Document.builder().id(master.getId()).fields(fields).build());
		}
		commands.flushCommands();
	}

}
