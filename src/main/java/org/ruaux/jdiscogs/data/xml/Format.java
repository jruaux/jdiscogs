package org.ruaux.jdiscogs.data.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "format")
@XmlAccessorType(XmlAccessType.FIELD)
public class Format {

	@XmlAttribute(name = "name")
	private String name;
	@XmlAttribute(name = "qty")
	private Integer qty;
	@XmlAttribute(name = "text")
	private String text;
	@XmlElement(name = "descriptions")
	private Descriptions descriptions;

}