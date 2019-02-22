package org.ruaux.jdiscogs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ruaux.jdiscogs.api.DiscogsClient;
import org.ruaux.jdiscogs.api.model.Master;
import org.ruaux.jdiscogs.api.model.Release;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { JDiscogsConfiguration.class, DiscogsClient.class})
public class JDiscogsClientTests {

	@Autowired
	private DiscogsClient client;

	@Test
	public void testMaster() {
		Master master = client.getMaster("16969");
		assertEquals("The Royal Scam", master.getTitle());
		assertEquals(2, master.getStyles().size());
		assertEquals("Pop Rock", master.getStyles().get(0));
		assertEquals("Jazz-Rock", master.getStyles().get(1));
		assertEquals(1, master.getGenres().size());
		assertEquals("Rock", master.getGenres().get(0));
		assertEquals(1, master.getArtists().size());
		assertEquals("Steely Dan", master.getArtists().get(0).getName());
		assertEquals(9, master.getTracklist().size());
		assertEquals("A1", master.getTracklist().get(0).getPosition());
		assertEquals("track", master.getTracklist().get(1).getType_());
		assertEquals("Don't Take Me Alive", master.getTracklist().get(2).getTitle());
		assertEquals("4:22", master.getTracklist().get(3).getDuration());
		assertEquals(10, master.getImages().size());
//		assertEquals(
//				"https://img.discogs.com/7VwTvQKoNtrUEwRK3qWS4xI7McY=/fit-in/600x598/filters:strip_icc():format(jpeg):mode_rgb():quality(90)/discogs-images/R-373262-1309152267.jpeg.jpg",
//				master.getImages().get(0).getUri());
	}

	@Test
	public void testRelease() {
		Release release = client.getRelease("9680548");
		assertEquals("The Royal Scam", release.getTitle());
		assertEquals(2, release.getGenres().size());
		assertEquals("Jazz", release.getGenres().get(0));
		assertEquals("Rock", release.getGenres().get(1));
		assertEquals(3, release.getStyles().size());
		assertEquals("Fusion", release.getStyles().get(0));
		assertEquals("Jazz-Funk", release.getStyles().get(1));
		assertEquals("Pop Rock", release.getStyles().get(2));
		assertEquals(1, release.getFormats().size());
		assertEquals("9", release.getFormats().get(0).getQty());
		assertEquals("FLAC", release.getFormats().get(0).getDescriptions().get(0));
		assertEquals("Album", release.getFormats().get(0).getDescriptions().get(1));
		assertEquals("Reissue", release.getFormats().get(0).getDescriptions().get(2));

	}

}
