package org.ruaux.jdiscogs.data.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "video")
@XmlAccessorType(XmlAccessType.FIELD)
public class Video {

	@XmlAttribute(name = "duration")
	private Integer duration;
	@XmlAttribute(name = "embed")
	private Boolean embed;
	@XmlAttribute(name = "src")
	private String src;
	@XmlElement(name = "title")
	private String title;
	@XmlElement(name = "description")
	private String description;

}
