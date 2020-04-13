package org.ruaux.jdiscogs.model;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public class Community {

	@Getter
	@Setter
	private String status;
	@Getter
	@Setter
	private Rating rating;
	@Getter
	@Setter
	private Long want;
	@Getter
	@Setter
	private List<User> contributors;
	@Getter
	@Setter
	private Long have;
	@Getter
	@Setter
	private User submitter;
	@Getter
	@Setter
	private String data_quality;
}
