package org.ruaux.jdiscogs.data;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import org.springframework.core.io.Resource;

import java.net.URL;

@ConfigurationProperties(prefix = "discogs")
public @Data class JDiscogsBatchProperties {

	private String hashArrayDelimiter = ",";
	private String releasesUrl = "https://discogs-data.s3-us-west-2.amazonaws.com/data/2019/discogs_20191201_releases.xml.gz";
	private String mastersUrl = "https://discogs-data.s3-us-west-2.amazonaws.com/data/2019/discogs_20191201_masters.xml.gz";
	private int batchSize = 50;
	private int threads = 1;
	private boolean forceLoad = false;
	private boolean noOp = false;
	private String releaseIndex = "releases";
	private Range releaseItemCount = Range.builder().min(10000000).max(Double.MAX_VALUE).build();
	private Range masterItemCount = Range.builder().min(1000000).max(Double.MAX_VALUE).build();
	private String masterIndex = "masters";
	private String artistSuggestionIndex = "artists";
	private int minImages = 2;
	private Range imageHeight = Range.builder().min(400).max(600).build();
	private Range imageWidth = Range.builder().min(400).max(600).build();
	private Range imageRatio = Range.builder().min(.95).max(1.05).build();

}