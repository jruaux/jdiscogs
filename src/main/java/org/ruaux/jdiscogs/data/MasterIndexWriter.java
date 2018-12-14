package org.ruaux.jdiscogs.data;

import java.util.List;

import org.ruaux.jdiscogs.JDiscogsConfiguration;
import org.ruaux.jdiscogs.data.xml.Artist;
import org.ruaux.jdiscogs.data.xml.Master;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.redislabs.springredisearch.RediSearchConfiguration;

import io.redisearch.Document;
import io.redisearch.Schema;
import io.redisearch.Suggestion;
import io.redisearch.client.Client;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.exceptions.JedisException;

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
	private RediSearchConfiguration rediSearchConfig;
	@Autowired
	private JDiscogsConfiguration config;
	private Client client;
	private Client artistSuggestionClient;

	@Override
	public void open(ExecutionContext executionContext) {
		this.client = rediSearchConfig.getClient(config.getMasterIndex());
		this.artistSuggestionClient = rediSearchConfig.getClient(config.getArtistSuggestionIndex());
		Schema schema = new Schema();
		schema.addSortableTextField(FIELD_ARTIST, 1);
		schema.addSortableTextField(FIELD_ARTISTID, 1);
		schema.addSortableTextField(FIELD_DATAQUALITY, 1);
		schema.addSortableTextField(FIELD_GENRES, 1);
		schema.addSortableTextField(FIELD_STYLES, 1);
		schema.addSortableTextField(FIELD_TITLE, 1);
		schema.addSortableNumericField(FIELD_YEAR);
		schema.addSortableTextField(FIELD_IMAGE, 1);
		try {
			client.createIndex(schema, Client.IndexOptions.Default());
		} catch (JedisException e) {
			if (log.isDebugEnabled()) {
				log.debug("Could not create index", e);
			} else {
				log.info("Could not create index, might already exist");
			}
		}
	}

	@Override
	public void write(List<? extends Master> items) throws Exception {
		Document[] docs = new Document[items.size()];
		for (int index = 0; index < docs.length; index++) {
			Master master = items.get(index);
			String masterId = master.getId();
			Document doc = new Document(masterId);
			if (master.getArtists() != null) {
				List<Artist> artists = master.getArtists().getArtists();
				if (artists != null && artists.size() > 0) {
					Artist artist = artists.get(0);
					doc.set(FIELD_ARTIST, artist.getName());
					doc.set(FIELD_ARTISTID, artist.getId());
					Suggestion suggestion = Suggestion.builder().str(artist.getName()).payload(artist.getId()).build();
					artistSuggestionClient.addSuggestion(suggestion, true);
				}
			}
			doc.set(FIELD_DATAQUALITY, master.getDataQuality());
			if (master.getGenres() != null) {
				List<String> genres = master.getGenres().getGenres();
				if (genres != null && genres.size() > 0) {
					doc.set(FIELD_GENRES, String.join(config.getHashArrayDelimiter(), genres));
				}
			}
			if (master.getStyles() != null) {
				List<String> styles = master.getStyles().getStyles();
				if (styles != null && styles.size() > 0) {
					doc.set(FIELD_STYLES, String.join(config.getHashArrayDelimiter(), styles));
				}
			}
			doc.set(FIELD_TITLE, master.getTitle());
			doc.set(FIELD_YEAR, master.getYear());
			doc.set(FIELD_IMAGE, master.getImages() != null && master.getImages().getImages().size() > 0);
			docs[index] = doc;
		}
		client.addDocuments(docs);
	}

}