package org.ruaux.jdiscogs;

import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ruaux.jdiscogs.data.Helper;
import org.ruaux.jdiscogs.data.NormalizedTrack;
import org.ruaux.jdiscogs.model.Release;

public class TestNormalizedRelease {

	private Release release(String resource) throws JAXBException {
		return (Release) JAXBContext.newInstance(Release.class).createUnmarshaller()
				.unmarshal(getClass().getClassLoader().getResourceAsStream(resource));
	}

	@Test
	public void testTracklist() throws JAXBException {
		Release release = release("release-4273080.xml");
		List<NormalizedTrack> tracks = normalizedTracks(release);
		Assertions.assertEquals(23, tracks.size());
		Assertions.assertEquals("He Ain't Heavy, He's My Brother - Bridge Over Troubled Water",
				tracks.get(16).getTitle());
	}

	@Test
	public void testVinyl() throws JAXBException {
		Release release = release("release-vinyl.xml");
		List<NormalizedTrack> tracks = normalizedTracks(release);
		Assertions.assertEquals(6, tracks.size());
	}

	@Test
	public void testVinyl2() throws JAXBException {
		Release release = release("release-vinyl-2.xml");
		List<NormalizedTrack> tracks = normalizedTracks(release);
		Assertions.assertEquals(10, tracks.size());
	}

	private List<NormalizedTrack> normalizedTracks(Release release) {
		return Helper.normalize(release).getTracks();
	}

	@Test
	public void testMultiDiscPositions() throws JAXBException {
		Release release = release("release-multidisc-positions.xml");
		List<NormalizedTrack> tracks = normalizedTracks(release);
		Assertions.assertEquals(51, tracks.size());
	}

	@Test
	public void testMultiDiscSections() throws JAXBException {
		Release release = release("release-multidisc-sections.xml");
		List<NormalizedTrack> tracks = normalizedTracks(release);
		Assertions.assertEquals(44, tracks.size());
	}

}
