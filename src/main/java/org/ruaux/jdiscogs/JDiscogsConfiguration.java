package org.ruaux.jdiscogs;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "discogs")
@EnableAutoConfiguration
@Data
public class JDiscogsConfiguration {

	private String url = "https://api.discogs.com/{entity}/{id}";
	private String token;
	private String userAgent = "com.redislabs.rediscogs.useragent";
	private int batchSize = 50;
	private boolean noopWriters = false;
	private String hashArrayDelimiter = ",";
	private String fileUrlTemplate = "https://discogs-data.s3-us-west-2.amazonaws.com/data/2018/discogs_20181001_{entity}s.xml.gz";
	private String releaseIndex = "releaseIdx";
	private String masterIndex = "masterIdx";
	private String artistSuggestionIndex = "artistSuggestIdx";

	@Bean
	public StringRedisTemplate redisTemplate(LettuceConnectionFactory connectionFactory) {
		StringRedisTemplate template = new StringRedisTemplate();
		template.setConnectionFactory(connectionFactory);
		return template;
	}

}