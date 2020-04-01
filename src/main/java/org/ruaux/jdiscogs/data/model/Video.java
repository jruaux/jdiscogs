package org.ruaux.jdiscogs.data.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "video")
@XmlAccessorType(XmlAccessType.FIELD)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public @Data class Video {

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
