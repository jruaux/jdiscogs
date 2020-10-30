package org.ruaux.jdiscogs;

import org.springframework.batch.item.redis.support.KeyMaker;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "discogs")
public class JDiscogsProperties {

	private String token;
	private String userAgent = "jdiscogs.useragent";
	private String keySeparator = KeyMaker.DEFAULT_SEPARATOR;
	private int batch = 50;
	private int threads = 1;
	private boolean noOp;
	private MasterImportOptions masters = new MasterImportOptions();
	private ReleaseImportOptions releases = new ReleaseImportOptions();

	@Data
	public static class MasterImportOptions {

		public static final String DEFAULT_PREFIX = "master";
		public static final String DEFAULT_INDEX = "masters";

		private String url = "https://discogs-data.s3-us-west-2.amazonaws.com/data/2020/discogs_20201001_masters.xml.gz";
		private String index = DEFAULT_INDEX;
		private String prefix = DEFAULT_PREFIX;
		private int minImageHeight = 400;
		private int minImageWidth = 400;
		private double imageRatioTolerance = .05;

	}

	@Data
	public static class ReleaseImportOptions {

		public static final String DEFAULT_PREFIX = "release";
		public static final String DEFAULT_INDEX = "releases";

		private String url = "https://discogs-data.s3-us-west-2.amazonaws.com/data/2020/discogs_20201001_releases.xml.gz";
		private String index = DEFAULT_INDEX;
		private String prefix = DEFAULT_PREFIX;

	}

}
