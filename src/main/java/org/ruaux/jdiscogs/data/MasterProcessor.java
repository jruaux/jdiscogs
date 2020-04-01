package org.ruaux.jdiscogs.data;

import static org.ruaux.jdiscogs.data.Fields.ARTIST;
import static org.ruaux.jdiscogs.data.Fields.ARTIST_ID;
import static org.ruaux.jdiscogs.data.Fields.GENRES;
import static org.ruaux.jdiscogs.data.Fields.TITLE;
import static org.ruaux.jdiscogs.data.Fields.YEAR;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.ruaux.jdiscogs.data.model.Artist;
import org.ruaux.jdiscogs.data.model.Image;
import org.ruaux.jdiscogs.data.model.Master;
import org.springframework.batch.item.ItemProcessor;

import com.redislabs.lettusearch.search.Document;

import lombok.Builder;
import lombok.Setter;

@Builder
public class MasterProcessor implements ItemProcessor<Master, Document<String, String>> {

	private @Setter JDiscogsBatchProperties props;
	private @Setter XmlCodec codec;

	@Override
	public Document<String, String> process(Master master) throws Exception {
		if (!hasImage(master) || !hasYear(master)) {
			return null;
		}
		Document<String, String> doc = Document.<String, String>builder().id(master.getId()).score(1d).build();
		if (master.getArtists() != null && !master.getArtists().getArtists().isEmpty()) {
			Artist artist = master.getArtists().getArtists().get(0);
			if (artist != null) {
				doc.put(ARTIST, artist.getName());
				doc.put(ARTIST_ID, artist.getId());
			}
		}
		Set<String> genres = new LinkedHashSet<>();
		if (master.getGenres() != null && master.getGenres().getGenres() != null) {
			genres.addAll(master.getGenres().getGenres());
		}
		if (master.getStyles() != null && master.getStyles().getStyles() != null) {
			genres.addAll(master.getStyles().getStyles());
		}
		doc.put(GENRES, String.join(props.getHashArrayDelimiter(), sanitize(genres)));
		doc.put(TITLE, master.getTitle());
		doc.put(YEAR, master.getYear());
		doc.setPayload(codec.getXml(master));
		return doc;
	}

	private boolean hasYear(Master master) {
		return master.getYear() != null && master.getYear().length() >= 4;
	}

	private boolean hasImage(Master master) {
		Image image = master.getPrimaryImage();
		if (image == null) {
			return false;
		}
		return withinRange(image.getHeight(), props.getImageHeight())
				&& withinRange(image.getWidth(), props.getImageWidth())
				&& withinRange(image.getRatio(), props.getImageRatio());
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
