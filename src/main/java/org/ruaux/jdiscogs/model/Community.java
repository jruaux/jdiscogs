package org.ruaux.jdiscogs.model;

import java.util.List;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Community {

	private String status;
	private Rating rating;
	private Long want;
	@XmlElement(name="contributor")
	@XmlElementWrapper(name="contributors")
	private List<User> contributors;
	private Long have;
	private User submitter;
	private String data_quality;
}
