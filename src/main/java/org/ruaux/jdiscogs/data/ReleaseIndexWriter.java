package org.ruaux.jdiscogs.data;

import java.util.List;

import org.ruaux.jdiscogs.JDiscogsConfiguration;
import org.ruaux.jdiscogs.data.xml.Artist;
import org.ruaux.jdiscogs.data.xml.Release;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.redislabs.springredisearch.RediSearchConfiguration;

import io.redisearch.Document;
import io.redisearch.Schema;
import io.redisearch.client.AddOptions;
import io.redisearch.client.Client;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.exceptions.JedisException;

@Component
@Slf4j
public class ReleaseIndexWriter extends ItemStreamSupport implements ItemWriter<Release> {

	public static final String FIELD_TITLE = "title";
	public static final String FIELD_ARTIST = "artist";
	@Autowired
	private JDiscogsConfiguration config;
	@Autowired
	private RediSearchConfiguration rediSearchConfig;
	private Client client;

	@Override
	public void open(ExecutionContext executionContext) {
		this.client = rediSearchConfig.getClient(config.getReleaseIndex());
		Schema schema = new Schema();
		schema.addSortableTextField(FIELD_ARTIST, 1);
		schema.addSortableTextField(FIELD_TITLE, 1);
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
	public void write(List<? extends Release> items) throws Exception {
		Document[] docs = new Document[items.size()];
		for (int index = 0; index < docs.length; index++) {
			Release release = items.get(index);
			String releaseId = release.getId();
			Document doc = new Document(releaseId);
			if (release.getArtists() != null) {
				List<Artist> artists = release.getArtists().getArtists();
				if (artists != null && artists.size() > 0) {
					doc.set(FIELD_ARTIST, artists.get(0));
				}
			}
			doc.set(FIELD_TITLE, release.getTitle());
			docs[index] = doc;
		}
		client.addDocuments(new AddOptions().setNosave(), docs);
	}

}
