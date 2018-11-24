package org.ruaux.jdiscogs.model;

import java.util.List;

import lombok.Data;

@Data
public class Community {

	private String status;
	private Rating rating;
	private Integer want;
	private List<Contributor> contributor;
	private Integer have;
	private Submitter submitter;
	private String dataQuality;
}
