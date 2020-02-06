package org.ruaux.jdiscogs.data;

import org.ruaux.jdiscogs.data.JDiscogsBatchConfiguration.LoadJob;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "discogs")
public @Data class JDiscogsBatchProperties {

	private String hashArrayDelimiter = ",";
	private String dataUrl = "https://discogs-data.s3-us-west-2.amazonaws.com/data/2019/discogs_20191201_{entity}s.xml.gz";
	private int batchSize = 50;
	private LoadJob[] jobs = { LoadJob.MasterDocsIndex };
	private int threads = 1;
	private boolean skip = false;
	private boolean noOp = false;
	private String releaseIndex = "releases";
	private String masterIndex = "masters";
	private String artistSuggestionIndex = "artists";
	private double imageRatioMin = .9;
	private double imageRatioMax = 1.1;
	private int minImages = 2;
	private Range imageHeight = new Range(400, 600);
	private Range imageWidth = new Range(400, 600);
	private Range imageRatio = new Range(.95, 1.05);

}