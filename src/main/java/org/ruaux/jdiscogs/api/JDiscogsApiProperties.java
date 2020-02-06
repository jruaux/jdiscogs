package org.ruaux.jdiscogs.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "discogs.api")
public @Data class JDiscogsApiProperties {

	private String url = "https://api.discogs.com/{entity}/{id}";
	private String token;
	private String userAgent = JDiscogsApiProperties.class.getPackage().getName() + ".useragent";

}
