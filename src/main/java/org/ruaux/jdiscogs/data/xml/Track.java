package org.ruaux.jdiscogs.data.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "track")
@XmlAccessorType(XmlAccessType.FIELD)
public class Track {

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

}
