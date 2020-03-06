package org.ruaux.jdiscogs.data.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "image")
@XmlAccessorType(XmlAccessType.FIELD)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public @Data class Image {

	public static final String TYPE_PRIMARY = "primary";
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

	public Double getRatio() {
		if (height == null) {
			return null;
		}
		if (width == null) {
			return null;
		}
		return height.doubleValue() / width.doubleValue();
	}

	public boolean isPrimary() {
		return TYPE_PRIMARY.equals(type);
	}

}