package org.ruaux.jdiscogs.data.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "identifier")
@XmlAccessorType(XmlAccessType.FIELD)
public class Identifier {

	@XmlAttribute(name = "description")
	private String description;
	@XmlAttribute(name = "type")
	private String type;
	@XmlAttribute(name = "value")
	private String value;

}