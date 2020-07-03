package org.ruaux.jdiscogs;

import com.redislabs.lettusearch.RediSearchClient;
import com.redislabs.lettusearch.RediSearchUtils;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.index.IndexInfo;
import io.lettuce.core.RedisURI;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ContextConfiguration(initializers = {JDiscogsIntegrationTests.JDiscogsInitializer.class})
@SpringBootTest(classes = JDiscogsApplication.class)
public class JDiscogsIntegrationTests {

    @Container
    private static GenericContainer rediSearch = new GenericContainer("redislabs/redisearch:latest").withExposedPorts(6379);

    static class JDiscogsInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of("spring.redis.host=" + rediSearch.getHost(), "spring.redis.port=" + rediSearch.getFirstMappedPort()).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    @Autowired
    JDiscogsProperties props;

    private RediSearchClient client() {
        return RediSearchClient.create(RedisURI.create(rediSearch.getHost(), rediSearch.getFirstMappedPort()));
    }

    @Test
    public void testReleasesJob() {
        IndexInfo info = RediSearchUtils.getInfo(client().connect().sync().ftInfo(props.getReleaseIndex()));
        Long numDocs = info.getNumDocs();
        Assertions.assertEquals(21, numDocs);
    }


}