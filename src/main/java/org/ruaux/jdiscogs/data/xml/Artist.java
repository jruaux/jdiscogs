package org.ruaux.jdiscogs.data.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "artist")
@XmlAccessorType(XmlAccessType.FIELD)
public class Artist {

	@XmlElement(name = "id")
	private String id;
	@XmlElement(name = "name")
	private String name;
	@XmlElement(name = "anv")
	private String anv;
	@XmlElement(name = "join")
	private String join;
	@XmlElement(name = "resource_url")
	private String resource_url;
	@XmlElement(name = "role")
	private String role;
	@XmlElement(name = "tracks")
	private String tracks;

}