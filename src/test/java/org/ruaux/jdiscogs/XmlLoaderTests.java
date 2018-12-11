package org.ruaux.jdiscogs;

import static org.junit.Assert.assertEquals;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;
import org.ruaux.jdiscogs.data.xml.Release;
import org.ruaux.jdiscogs.data.xml.Track;

public class XmlLoaderTests {

	@Test
	public void testReleases() throws Exception {
		JAXBContext context = JAXBContext.newInstance(Release.class);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		Release release = (Release) unmarshaller
				.unmarshal(this.getClass().getClassLoader().getResourceAsStream("release-4210378.xml"));
		assertEquals(14, release.getTrackList().getTracks().size());
		Track bonusHeading = release.getTrackList().getTracks().get(10);
		assertEquals("Bonus Tracks", bonusHeading.getTitle());
		assertEquals("", bonusHeading.getPosition());
		assertEquals("", bonusHeading.getDuration());
	}

}
