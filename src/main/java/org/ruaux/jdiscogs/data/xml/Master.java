package org.ruaux.jdiscogs.data.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "master")
@XmlAccessorType(XmlAccessType.FIELD)
@NoArgsConstructor
@AllArgsConstructor
public @Data class Master {

	@XmlAttribute(name = "id")
	private String id;
	@XmlElement(name = "artists")
	private Artists artists;
	@XmlElement(name = "data_quality")
	private String dataQuality;
	@XmlElement(name = "genres")
	private Genres genres;
	@XmlElement(name = "images")
	private Images images;
	@XmlElement(name = "notes")
	private String notes;
	@XmlElement(name = "styles")
	private Styles styles;
	@XmlElement(name = "title")
	private String title;
	@XmlElement(name = "year")
	private String year;
	@XmlElement(name = "videos")
	private Videos videos;

	public Image getPrimaryImage() {
		if (images == null) {
			return null;
		}
		if (images.getImages() == null) {
			return null;
		}
		return images.getImages().stream().filter(image -> image.isPrimary()).findFirst().orElse(null);
	}

}