package org.ruaux.jdiscogs.data.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlAccessorType(XmlAccessType.FIELD)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public @Data class MasterId {

	@XmlValue
	private String masterId;
	@XmlAttribute(name = "is_main_release")
	private Boolean mainRelease;

}