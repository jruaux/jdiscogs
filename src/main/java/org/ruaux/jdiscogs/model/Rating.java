package org.ruaux.jdiscogs.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class Rating {

	@Getter
	@Setter
	private Long count;
	@Getter
	@Setter
	private Double average;
}
