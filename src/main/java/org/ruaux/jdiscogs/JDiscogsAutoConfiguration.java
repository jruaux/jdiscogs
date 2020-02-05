package org.ruaux.jdiscogs;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JDiscogsProperties.class)
public @Data class JDiscogsAutoConfiguration {

}