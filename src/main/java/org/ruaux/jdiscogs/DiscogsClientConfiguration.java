package org.ruaux.jdiscogs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;

public class DiscogsClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "discogsClient")
    public DiscogsClient discogsClient(JDiscogsProperties props, RestTemplateBuilder restTemplateBuilder) {
        return new DiscogsClient(props.getApi(), restTemplateBuilder.build());
    }

}
