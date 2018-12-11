package org.ruaux.jdiscogs.data.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Identifiers {

	@XmlElement(name = "identifier")
	private List<Identifier> identifiers;

}