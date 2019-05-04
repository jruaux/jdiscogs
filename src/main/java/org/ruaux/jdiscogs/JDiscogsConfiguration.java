package org.ruaux.jdiscogs;

import org.ruaux.jdiscogs.data.BatchConfiguration.LoadJob;
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

	private DataConfiguration data = new DataConfiguration();
	private ApiConfiguration api = new ApiConfiguration();
	private String hashArrayDelimiter = ",";

	@Bean
	public StringRedisTemplate redisTemplate(LettuceConnectionFactory connectionFactory) {
		StringRedisTemplate template = new StringRedisTemplate();
		template.setConnectionFactory(connectionFactory);
		return template;
	}

	@Data
	public static class DataConfiguration {
		private String url = "https://discogs-data.s3-us-west-2.amazonaws.com/data/2019/discogs_20190402_{entity}s.xml.gz";
		private int batchSize = 50;
		private LoadJob[] jobs = { LoadJob.MasterDocsIndex };
		private boolean skip = false;
		private boolean noOp = false;
		private String releaseIndex = "releases";
		private String masterIndex = "masters";
		private String artistSuggestionIndex = "artists";
		private double imageRatioMin = .9;
		private double imageRatioMax = 1.1;
	}

	@Data
	public static class ApiConfiguration {
		private String url = "https://api.discogs.com/{entity}/{id}";
		private String token;
		private String userAgent = "com.redislabs.rediscogs.useragent";
	}

}