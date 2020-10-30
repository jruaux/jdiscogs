package org.ruaux.jdiscogs.data;

import static org.ruaux.jdiscogs.data.Fields.ARTIST;
import static org.ruaux.jdiscogs.data.Fields.DURATION;
import static org.ruaux.jdiscogs.data.Fields.ID;
import static org.ruaux.jdiscogs.data.Fields.TITLE;
import static org.ruaux.jdiscogs.data.Fields.TRACK;
import static org.ruaux.jdiscogs.data.Fields.TRACKS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ruaux.jdiscogs.model.Release;
import org.springframework.core.convert.converter.Converter;

public class ReleaseToMapConverter implements Converter<Release, Map<String, String>> {

	@Override
	public Map<String, String> convert(Release release) {
		Map<String, String> doc = new HashMap<>();
		NormalizedRelease normalizedRelease = Helper.normalize(release);
		doc.put(ARTIST, Helper.sanitize(normalizedRelease.getArtist()));
		doc.put(TITLE, Helper.sanitize(normalizedRelease.getTitle()));
		doc.put(ID, String.valueOf(normalizedRelease.getId()));
		List<NormalizedTrack> tracks = normalizedRelease.getTracks();
		doc.put(TRACKS, String.valueOf(tracks.size()));
		for (int index = 1; index < tracks.size(); index++) {
			NormalizedTrack track = tracks.get(index);
			String prefix = TRACK + "[" + index + "].";
			doc.put(prefix + TITLE, track.getTitle());
			doc.put(prefix + DURATION, String.valueOf(track.getDuration()));
		}
		return doc;
	}

}
