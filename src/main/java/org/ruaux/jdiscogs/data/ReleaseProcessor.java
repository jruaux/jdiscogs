package org.ruaux.jdiscogs.data;

import static org.ruaux.jdiscogs.data.Fields.ARTIST;
import static org.ruaux.jdiscogs.data.Fields.ID;
import static org.ruaux.jdiscogs.data.Fields.TITLE;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBException;

import org.ruaux.jdiscogs.data.model.Artist;
import org.ruaux.jdiscogs.data.model.Release;
import org.springframework.batch.item.ItemProcessor;

import com.redislabs.lettusearch.search.Document;

import lombok.Builder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class ReleaseProcessor implements ItemProcessor<Release, Document<String, String>> {

	@Builder.Default
	private @Setter TextSanitizer sanitizer = new TextSanitizer();
	private @Setter XmlCodec codec;

	@Override
	public Document<String, String> process(Release release) {
		Document<String, String> doc = Document.<String, String>builder().id(release.getId()).score(1d).build();
		Stream<String> artists = release.getArtists().getArtists().stream().map(Artist::getName);
		doc.put(ARTIST, sanitizer.sanitize(String.join(" ", artists.collect(Collectors.toList()))));
		doc.put(TITLE, sanitizer.sanitize(release.getTitle()));
		doc.put(ID, release.getId());
		try {
			doc.setPayload(codec.getXml(release));
		} catch (JAXBException e) {
			log.error("Could not marshall release {} to XML", release.getId(), e);
		}
		return doc;
	}

}
