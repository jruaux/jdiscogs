package org.ruaux.jdiscogs.data;

import static org.ruaux.jdiscogs.data.Fields.ARTIST;
import static org.ruaux.jdiscogs.data.Fields.ARTIST_ID;
import static org.ruaux.jdiscogs.data.Fields.GENRES;
import static org.ruaux.jdiscogs.data.Fields.JSON;
import static org.ruaux.jdiscogs.data.Fields.TITLE;
import static org.ruaux.jdiscogs.data.Fields.YEAR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ruaux.jdiscogs.JDiscogsProperties;
import org.ruaux.jdiscogs.Utils;
import org.ruaux.jdiscogs.model.Artist;
import org.ruaux.jdiscogs.model.Image;
import org.ruaux.jdiscogs.model.Master;
import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MasterToMapConverter implements Converter<Master, Map<String, String>> {

	private final ObjectMapper mapper = new ObjectMapper();
	private final JDiscogsProperties props;

	public MasterToMapConverter(JDiscogsProperties props) {
		this.props = props;
	}

	@Override
	public Map<String, String> convert(Master master) {
		if (!hasImage(master) || master.getYear() == null) {
			return null;
		}
		Map<String, String> doc = new HashMap<>();
		if (master.getArtists() != null && !master.getArtists().isEmpty()) {
			Artist artist = master.getArtists().get(0);
			if (artist != null) {
				doc.put(ARTIST, artist.getName());
				doc.put(ARTIST_ID, String.valueOf(artist.getId()));
			}
		}
		Set<String> genres = new LinkedHashSet<>();
		genres.addAll(master.getGenres());
		genres.addAll(master.getStyles());
		doc.put(GENRES, String.join(Helper.ARRAY_SEPARATOR, sanitize(genres)));
		doc.put(TITLE, master.getTitle());
		doc.put(YEAR, String.valueOf(master.getYear()));
		try {
			doc.put(JSON, mapper.writeValueAsString(master));
		} catch (JsonProcessingException e) {
			log.error("Could not marshal master {}", master.getId(), e);
		}
		return doc;
	}

	private boolean hasImage(Master master) {
		Image image = Utils.primaryImage(master);
		if (image == null) {
			return false;
		}
		return image.getHeight() >= props.getMasters().getMinImageHeight()
				&& image.getWidth() >= props.getMasters().getMinImageWidth()
				&& Math.abs(1 - ratio(image)) <= props.getMasters().getImageRatioTolerance();
	}

	private List<String> sanitize(Set<String> getGenres) {
		List<String> result = new ArrayList<>();
		getGenres.forEach(genre -> result.add(genre.replace(',', ' ')));
		return result;
	}

	private Double ratio(Image image) {
		if (image.getHeight() == null) {
			return 0d;
		}
		if (image.getWidth() == null) {
			return Double.MAX_VALUE;
		}
		return image.getHeight().doubleValue() / image.getWidth().doubleValue();
	}

}
