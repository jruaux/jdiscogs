package org.ruaux.jdiscogs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ruaux.jdiscogs.data.JDiscogsBatchConfiguration;
import org.ruaux.jdiscogs.data.JDiscogsBatchConfiguration.LoadJob;
import org.ruaux.jdiscogs.data.JDiscogsBatchProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.redislabs.springredisearch.RediSearchAutoConfiguration;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { JDiscogsAutoConfiguration.class, RedisAutoConfiguration.class,
		RediSearchAutoConfiguration.class }, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebClient
public class ReleaseLoadTest {

	@Autowired
	private JDiscogsBatchConfiguration config;
	@Autowired
	private JDiscogsBatchProperties props;

	@Test
	public void testRelease() throws Exception {
		props.setDataUrl(
				"https://raw.githubusercontent.com/jruaux/jdiscogs/master/src/test/resources/releases-old.xml");
		config.run(LoadJob.Releases);
	}

}
