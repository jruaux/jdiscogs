package org.ruaux.jdiscogs.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public @Data class Range {
	private double min;
	private double max;

}