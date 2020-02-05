package org.ruaux.jdiscogs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.junit.jupiter.api.Test;
import org.ruaux.jdiscogs.data.xml.Release;
import org.ruaux.jdiscogs.data.xml.Track;

public class XmlLoaderTests {

	@Test
	public void testReleases() throws Exception {
		JAXBContext context = JAXBContext.newInstance(Release.class);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		Release release = (Release) unmarshaller
				.unmarshal(this.getClass().getClassLoader().getResourceAsStream("release-4210378.xml"));
		assertEquals(14, release.trackList().tracks().size());
		Track bonusHeading = release.trackList().tracks().get(10);
		assertEquals("Bonus Tracks", bonusHeading.title());
		assertEquals("", bonusHeading.position());
		assertEquals("", bonusHeading.duration());
	}

}
