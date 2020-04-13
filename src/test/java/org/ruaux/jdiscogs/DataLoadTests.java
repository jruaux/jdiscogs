package org.ruaux.jdiscogs;

import com.redislabs.lettusearch.RediSearchUtils;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.index.IndexInfo;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = JDiscogsApplication.class)
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class DataLoadTests {

	@Autowired
	private StatefulRediSearchConnection<String,String> connection;
	@Autowired
	private JDiscogsProperties props;

	@Before
	public void cleanup() {
		connection.sync().flushall();
	}

	@Test
	public void testMasterLoadJob() {
		IndexInfo info = RediSearchUtils.getInfo(connection.sync().ftInfo(props.getReleaseIndex()));
		Long numDocs = info.getNumDocs();
		Assertions.assertEquals(21, numDocs);
	}

}