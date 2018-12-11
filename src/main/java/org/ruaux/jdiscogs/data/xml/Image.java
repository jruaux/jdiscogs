package org.ruaux.jdiscogs.data.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "image")
@XmlAccessorType(XmlAccessType.FIELD)
public class Image {

	@XmlAttribute(name = "height")
	private Integer height;
	@XmlAttribute(name = "type")
	private String type;
	@XmlAttribute(name = "uri")
	private String uri;
	@XmlAttribute(name = "uri150")
	private String uri150;
	@XmlAttribute(name = "width")
	private Integer width;

}