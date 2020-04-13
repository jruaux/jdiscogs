package org.ruaux.jdiscogs;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.ruaux.jdiscogs.model.Release;
import org.ruaux.jdiscogs.model.Track;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.oxm.xstream.XStreamMarshaller;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = { JDiscogsApplication.class })
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class XmlLoaderTests {

	@Autowired
	XStreamMarshaller marshaller;

	@Test
	public void testReleases() throws Exception {
		Release release = (Release) loadXml("release-4210378.xml");
		assertEquals(14, release.getTracks().size());
		Track bonusHeading = release.getTracks().get(10);
		assertEquals("Bonus Tracks", bonusHeading.getTitle());
		assertEquals("", bonusHeading.getPosition());
		assertEquals("", bonusHeading.getDuration());
	}

	@SuppressWarnings("unchecked")
	private Object loadXml(String resourceName) throws IOException {
		return marshaller.unmarshalInputStream(getResourceAsStream(resourceName));
	}

	private InputStream getResourceAsStream(String resourceName) {
		return this.getClass().getClassLoader().getResourceAsStream(resourceName);
	}

	@Test
	public void testReleaseSubtracks() throws Exception {
		Release release = (Release) loadXml("release-9536040.xml");
		Track track = release.getTracklist().get(6);
		List<Track> subTracks = track.getSub_tracks();
		assertEquals(4, subTracks.size());

	}

}
