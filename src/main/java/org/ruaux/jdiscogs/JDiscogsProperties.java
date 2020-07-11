package org.ruaux.jdiscogs;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "discogs")
public class JDiscogsProperties {

	public static final String DEFAULT_RELEASE_INDEX = "releases";
	public static final String DEFAULT_MASTER_INDEX = "masters";
	public static final String DEFAULT_ARTIST_SUGGEST_INDEX = "artists";

	private String apiUrl = "https://api.discogs.com/{entity}/{id}";
	private String token;
	private String userAgent = "jdiscogs.useragent";
	private String arraySeparator = ",";
	private String releasesUrl = "https://discogs-data.s3-us-west-2.amazonaws.com/data/2020/discogs_20200703_releases.xml.gz";
	private String mastersUrl = "https://discogs-data.s3-us-west-2.amazonaws.com/data/2020/discogs_20200703_masters.xml.gz";
	private int batchSize = 50;
	private int threads = 1;
	private boolean forceLoad = false;
	private boolean noOp = false;
	private String releaseIndex = DEFAULT_RELEASE_INDEX;
	private int minReleaseItemCount = 10000000;
	private int minMasterItemCount = 1000000;
	private String masterIndex = DEFAULT_MASTER_INDEX;
	private String artistSuggestionIndex = DEFAULT_ARTIST_SUGGEST_INDEX;
	private int minImages = 2;
	private int minImageHeight = 400;
	private int minImageWidth = 400;
	private double imageRatioTolerance = .05;

}
