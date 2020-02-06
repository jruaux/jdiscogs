package org.ruaux.jdiscogs;

import org.ruaux.jdiscogs.api.DiscogsClient;
import org.ruaux.jdiscogs.api.JDiscogsApiProperties;
import org.ruaux.jdiscogs.data.JDiscogsBatchConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JDiscogsApiProperties.class)
@Import(JDiscogsBatchConfiguration.class)
public class JDiscogsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "discogsClient")
	public DiscogsClient discogsClient(JDiscogsApiProperties props, RestTemplateBuilder restTemplateBuilder) {
		DiscogsClient client = new DiscogsClient();
		client.setProps(props);
		client.setRestTemplate(restTemplateBuilder.build());
		return client;
	}

}
