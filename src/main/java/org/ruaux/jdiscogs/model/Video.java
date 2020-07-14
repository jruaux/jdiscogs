package org.ruaux.jdiscogs.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlRootElement(name = "video")
@XmlAccessorType(XmlAccessType.FIELD)
public class Video {

	@XmlAttribute
	private Integer duration;
	@XmlAttribute
	private boolean embed;
	@XmlAttribute(name = "src")
	private String uri;
	private String title;
	private String description;

}
