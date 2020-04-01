package org.ruaux.jdiscogs.data.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "track")
@XmlAccessorType(XmlAccessType.FIELD)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public @Data class Track {

	@XmlElement(name = "position")
	private String position;
	@XmlElement(name = "title")
	private String title;
	@XmlElement(name = "duration")
	private String duration;
	@XmlElement(name = "extraartists")
	private Artists extraartists;
	@XmlElement(name = "artists")
	private Artists artists;
	@XmlElement(name = "sub_tracks")
	private SubTrackList subTrackList;

	public String getArtist() {
		if (artists == null || artists.getArtists() == null || artists.getArtists().isEmpty()) {
			return null;
		}
		return artists.getArtists().get(0).getName();
	}

}
