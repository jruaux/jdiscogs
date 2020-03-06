package org.ruaux.jdiscogs.data.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

@XmlAccessorType(XmlAccessType.FIELD)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public @Data class Images {

	@XmlElement(name = "image")
	@Singular
	private List<Image> images;

}
