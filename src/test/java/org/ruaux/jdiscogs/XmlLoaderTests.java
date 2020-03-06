package org.ruaux.jdiscogs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ruaux.jdiscogs.data.JDiscogsBatchConfiguration;
import org.ruaux.jdiscogs.data.xml.Release;
import org.ruaux.jdiscogs.data.xml.Track;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.redislabs.springredisearch.RediSearchAutoConfiguration;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { JDiscogsBatchConfiguration.class, RedisAutoConfiguration.class,
		RediSearchAutoConfiguration.class }, webEnvironment = WebEnvironment.RANDOM_PORT)
public class XmlLoaderTests {

	@Test
	public void testReleases() throws Exception {
		Release release = loadXml("release-4210378.xml", Release.class);
		assertEquals(14, release.getTrackList().getTracks().size());
		Track bonusHeading = release.getTrackList().getTracks().get(10);
		assertEquals("Bonus Tracks", bonusHeading.getTitle());
		assertEquals("", bonusHeading.getPosition());
		assertEquals("", bonusHeading.getDuration());
	}

	@SuppressWarnings("unchecked")
	private <T> T loadXml(String resourceName, Class<T> clazz) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(clazz);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		return (T) unmarshaller.unmarshal(getResourceAsStream(resourceName));
	}

	private InputStream getResourceAsStream(String resourceName) {
		return this.getClass().getClassLoader().getResourceAsStream(resourceName);
	}

	@Test
	public void testReleaseSubtracks() throws Exception {
		Release release = loadXml("releases.xml", Release.class);
		Track track = release.getTrackList().getTracks().get(6);
		List<Track> subTracks = track.getSubTrackList().getTracks();
		assertEquals(4, subTracks.size());

	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testJackson() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper xmlmapper = new XmlMapper();
		Map map = xmlmapper.readValue(getResourceAsStream("releases.xml"), Map.class);
		System.out.println(map);
	}

}
