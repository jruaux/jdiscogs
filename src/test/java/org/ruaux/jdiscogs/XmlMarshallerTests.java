package org.ruaux.jdiscogs;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.ruaux.jdiscogs.model.*;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.junit4.SpringRunner;

import javax.xml.bind.JAXBException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(SpringRunner.class)
public class XmlMarshallerTests {

    @Test
    public void testRelease() throws JAXBException {
        Release release = release("release-1.xml");
        assertEquals(4, release.getImages().size());
        Image image = release.getImages().get(0);
        assertEquals(600, image.getHeight());
        assertEquals("primary", image.getType());
        assertEquals("", image.getUri());
        assertEquals("", image.getUri150());
        assertEquals(600, image.getWidth());
        assertEquals(1, release.getArtists().size());
        Artist artist = release.getArtists().get(0);
        assertEquals(1L, artist.getId());
        assertEquals("The Persuader", artist.getName());
        assertEquals("Stockholm", release.getTitle());
        Label label = release.getLabels().get(0);
        assertEquals("SK032", label.getCatno());
        assertEquals(5L, label.getId());
        assertEquals("Svek", label.getName());
        Artist extraArtist = release.getExtraartists().get(0);
        assertEquals(239L, extraArtist.getId());
        assertEquals("Jesper Dahlbäck", extraArtist.getName());
        assertEquals("Music By [All Tracks By]", extraArtist.getRole());
        Format format = release.getFormats().get(0);
        assertEquals("Vinyl", format.getName());
        assertEquals(2, format.getQty());
        assertEquals(Arrays.asList("12\"", "33 ⅓ RPM"), format.getDescriptions());
        assertEquals(Collections.singletonList("Electronic"), release.getGenres());
        assertEquals(Collections.singletonList("Deep House"), release.getStyles());
        assertEquals("Sweden", release.getCountry());
        assertEquals("1999-03-00", release.getReleased());
        assertEquals(1660109, release.getMaster_id().getId());
        assertEquals(6, release.getTracklist().size());
        Track track = release.getTracklist().get(0);
        assertEquals("A", track.getPosition());
        assertEquals("Östermalm", track.getTitle());
        assertEquals("4:45", track.getDuration());
        assertEquals(5, release.getIdentifiers().size());
        Identifier identifier = release.getIdentifiers().get(0);
        assertEquals("A-Side Runout", identifier.getDescription());
        assertEquals("Matrix / Runout", identifier.getType());
        assertEquals("MPO SK 032 A1", identifier.getValue());
        assertEquals(6, release.getVideos().size());
        Video video = release.getVideos().get(0);
        assertEquals(296, video.getDuration());
        assertTrue(video.isEmbed());
        assertEquals("https://www.youtube.com/watch?v=MpmbntGDyNE", video.getUri());
        assertEquals("The Persuader - Östermalm", video.getTitle());
        assertEquals(2, release.getCompanies().size());
        Company company = release.getCompanies().get(0);
        assertEquals(271046L, company.getId());
        assertEquals("The Globe Studios", company.getName());
        assertEquals("23", company.getEntity_type());
        assertEquals("Recorded At", company.getEntity_type_name());
        assertEquals("https://api.discogs.com/labels/271046", company.getResource_url());
    }

    private Release release(String resource) throws JAXBException {
        return (Release) marshaller().createUnmarshaller().unmarshal(getClass().getClassLoader().getResourceAsStream(resource));
    }

    private Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(Release.class);
        return marshaller;
    }

    @Test
    public void testMarshalling() throws JAXBException {
        Release left = release("release-4210378.xml");
        StringWriter writer = new StringWriter();
        marshaller().createMarshaller().marshal(left, writer);
        StringReader reader = new StringReader(writer.toString());
        Release right = (Release) marshaller().createUnmarshaller().unmarshal(reader);
        assertEquals(left, right);
    }

    @Test
    public void testTracklist() throws JAXBException {
        Release release = release("release-4273080.xml");
        List<NormalizedTrack> tracks = ReleaseUtils.normalizedTracks(release);
        assertEquals(23, tracks.size());
        assertEquals("He Ain't Heavy, He's My Brother / Bridge Over Troubled Water", tracks.get(16).getTitle());
    }


    @Test
    public void testMultiDiscPositions() throws JAXBException {
        Release release = release("release-multidisc-positions.xml");
        List<NormalizedTrack> tracks = ReleaseUtils.normalizedTracks(release);
        assertEquals(51, tracks.size());
        NormalizedTrack track17Disc3 = tracks.get(tracks.size() - 1);
        assertEquals(3, track17Disc3.getPosition().getDisc());
    }

    @Test
    public void testMultiDiscSections() throws JAXBException {
        Release release = release("release-multidisc-sections.xml");
        List<NormalizedTrack> tracks = ReleaseUtils.normalizedTracks(release);
        assertEquals(44, tracks.size());
        assertEquals(2, tracks.get(23).getPosition().getDisc());
        assertEquals(1, tracks.get(23).getPosition().getNumber());
    }

    @Test
    public void testSeries() throws JAXBException {
        Release release = release("release-series.xml");
        assertEquals(1, release.getSeries().size());
        assertEquals("Pulp Fusion", release.getSeries().get(0).getName());
        assertEquals(486989, release.getSeries().get(0).getId());
    }


}
