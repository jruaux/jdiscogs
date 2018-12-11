package org.ruaux.jdiscogs.data.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "label")
@XmlAccessorType(XmlAccessType.FIELD)
public  class Label {

	@XmlAttribute(name = "id")
	private String id;
	@XmlAttribute(name = "catno")
	private String catno;
	@XmlAttribute(name = "name")
	private String name;

}