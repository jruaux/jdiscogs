package org.ruaux.jdiscogs.data.xml;

import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.data.redis.core.RedisHash;

import lombok.Data;

@Data
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
@RedisHash("release")
public class Release {

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
	private TrackList trackList = new TrackList();
	@XmlElement(name = "identifiers")
	private Identifiers identifiers;
	@XmlElement(name = "companies")
	private Companies companies;

	public List<Track> getTracks() {
		return trackList.getTracks().stream()
				.filter(track -> track.getPosition() != null && track.getPosition().length() > 0)
				.collect(Collectors.toList());
	}

}
