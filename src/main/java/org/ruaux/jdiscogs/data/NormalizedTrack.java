package org.ruaux.jdiscogs.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NormalizedTrack {

	private String title;
	private long duration;

}
