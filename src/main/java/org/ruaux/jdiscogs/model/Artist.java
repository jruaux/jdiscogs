package org.ruaux.jdiscogs.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public class Artist extends IdResource {

	@Getter
	@Setter
	private String join;
	@Getter
	@Setter
	private String name;
	@Getter
	@Setter
	private String anv;
	@Getter
	@Setter
	private String tracks;
	@Getter
	@Setter
	private String role;

}