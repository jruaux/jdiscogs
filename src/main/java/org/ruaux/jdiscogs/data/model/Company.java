package org.ruaux.jdiscogs.data.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "company")
@XmlAccessorType(XmlAccessType.FIELD)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public @Data class Company {

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