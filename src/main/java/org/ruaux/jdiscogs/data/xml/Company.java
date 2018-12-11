package org.ruaux.jdiscogs.data.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "company")
@XmlAccessorType(XmlAccessType.FIELD)
public class Company {

	@XmlElement(name = "id")
	private String id;
	@XmlElement(name = "name")
	private String name;
	@XmlElement(name = "catno")
	private String catno;
	@XmlElement(name = "entity_type")
	private String entityType;
	@XmlElement(name = "entity_type_name")
	private String entityTypeName;
	@XmlElement(name = "resource_url")
	private String resourceUrl;

}