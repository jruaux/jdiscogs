package org.ruaux.jdiscogs.data;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NormalizedRelease {

	public static enum Format {
		CD, VINYL, OTHER, NONE
	}

	private long id;
	private String artist;
	private String title;
	private Format format;
	private List<NormalizedTrack> tracks;

}
