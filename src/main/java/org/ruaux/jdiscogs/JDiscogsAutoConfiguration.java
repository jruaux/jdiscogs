package org.ruaux.jdiscogs;

import com.redislabs.springredisearch.RediSearchAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.ruaux.jdiscogs.data.MasterProcessor;
import org.ruaux.jdiscogs.data.ReleaseProcessor;
import org.ruaux.jdiscogs.data.TextSanitizer;
import org.ruaux.jdiscogs.model.Master;
import org.ruaux.jdiscogs.model.Release;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Slf4j
@EnableConfigurationProperties(JDiscogsProperties.class)
@Import({BatchConfiguration.class, DiscogsClientConfiguration.class, RestTemplateAutoConfiguration.class, RediSearchAutoConfiguration.class, ReleaseProcessor.class, MasterProcessor.class, TextSanitizer.class})
public class JDiscogsAutoConfiguration {

    @Bean
    Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(Release.class, Master.class);
        return marshaller;
    }

    @Bean
    public static BeanFactoryPostProcessor beanFactoryPostProcessor() {
        return beanFactory -> beanFactory.registerScope("thread", new SimpleThreadScope());
    }

}
