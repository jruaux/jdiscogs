package org.ruaux.jdiscogs;

import com.redislabs.lettusearch.RediSearchUtils;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.index.IndexInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.ruaux.jdiscogs.data.JDiscogsBatchProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = JDiscogsTestApplication.class)
@RunWith(SpringRunner.class)
public class JDiscogsBatchIntegrationTests {

	@Autowired
	private StatefulRediSearchConnection<String,String> connection;
	@Autowired
	private JDiscogsBatchProperties props;

	@Test
	public void testMasterLoadJob() {
		IndexInfo info = RediSearchUtils.getInfo(connection.sync().ftInfo(props.getReleaseIndex()));
		Assertions.assertEquals(21, info.getNumDocs());
	}

}