package org.ruaux.jdiscogs;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "discogs")
public class JDiscogsProperties {

	private DiscogsApiOptions api = DiscogsApiOptions.builder().build();
	private DiscogsDataOptions data = DiscogsDataOptions.builder().build();

}
