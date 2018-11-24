package org.ruaux.jdiscogs;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "discogs")
@EnableAutoConfiguration
@Data
public class JDiscogsConfiguration {

	private String url = "https://api.discogs.com/{entity}/{id}";
	private String token;
	private String userAgent = "com.redislabs.rediscogs.useragent";

}