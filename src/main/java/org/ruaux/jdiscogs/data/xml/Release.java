package org.ruaux.jdiscogs.data.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.data.redis.core.RedisHash;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash
public @Data class Release {

	private static final Pattern POSITION_PATTERN = Pattern.compile("^([A-Z]|(\\d+[\\-\\:]))?\\d+$");

	@XmlAttribute(name = "id")
	private String id;
	@XmlAttribute(name = "status")
	private String status;
	@XmlElement(name = "images")
	private Images images;
	@XmlElement(name = "artists")
	private Artists artists;
	@XmlElement(name = "title")
	private String title;
	@XmlElement(name = "labels")
	private Labels labels;
	@XmlElement(name = "extraartists")
	private Artists extraArtists;
	@XmlElement(name = "formats")
	private Formats formats;
	@XmlElement(name = "genres")
	private Genres genres;
	@XmlElement(name = "styles")
	private Styles styles;
	@XmlElement(name = "country")
	private String country;
	@XmlElement(name = "released")
	private String released;
	@XmlElement(name = "notes")
	private String notes;
	@XmlElement(name = "data_quality")
	private String dataQuality;
	@XmlElement(name = "master_id")
	private MasterId masterId;
	@XmlElement(name = "tracklist")
	private TrackList trackList;
	@XmlElement(name = "identifiers")
	private Identifiers identifiers;
	@XmlElement(name = "companies")
	private Companies companies;

	public List<Track> getTracks() {
		return getTracks(trackList.getTracks());
	}

	private List<Track> getTracks(List<Track> tracks) {
		List<Track> actualTracks = new ArrayList<>();
		for (Track track : tracks) {
			if (track.getSubTrackList() == null || track.getSubTrackList().getTracks().isEmpty()) {
				if (POSITION_PATTERN.matcher(track.getPosition()).matches()) {
					actualTracks.add(track);
				}
			} else {
				actualTracks.addAll(getTracks(track.getSubTrackList().getTracks()));
			}
		}
		return actualTracks;
	}

	public String getArtist() {
		if (artists == null || artists.getArtists() == null || artists.getArtists().isEmpty()) {
			return null;
		}
		return artists.getArtists().get(0).getName();
	}

	public String getFormat() {
		if (formats == null || formats.getFormats() == null || formats.getFormats().isEmpty()) {
			return null;
		}
		return formats.getFormats().get(0).getName();

	}

}
