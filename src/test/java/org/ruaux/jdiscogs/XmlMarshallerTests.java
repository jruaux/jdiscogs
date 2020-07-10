package org.ruaux.jdiscogs;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.ruaux.jdiscogs.model.*;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.junit4.SpringRunner;

import javax.xml.bind.JAXBException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(SpringRunner.class)
public class XmlMarshallerTests {

    @Test
    public void testRelease() throws JAXBException {
        Release release = release("release-1.xml");
        assertEquals(4, release.getImages().size());
        assertEquals(Image.builder().height(600).type("primary").uri("").uri150("").width(600).build(), release.getImages().get(0));
        assertEquals(1, release.getArtists().size());
        assertEquals(Artist.builder().id(1L).name("The Persuader").anv("").join("").role("").tracks("").build(), release.getArtists().get(0));
        assertEquals("Stockholm", release.getTitle());
        assertEquals(Label.builder().catno("SK032").id(5L).name("Svek").build(), release.getLabels().get(0));
        assertEquals(Artist.builder().id(239L).name("Jesper Dahlbäck").anv("").join("").role("Music By [All Tracks By]").tracks("").build(), release.getExtraartists().get(0));
        assertEquals(Format.builder().name("Vinyl").qty(2).text("").descriptions(Arrays.asList("12\"", "33 ⅓ RPM")).build(), release.getFormats().get(0));
        assertEquals(Collections.singletonList("Electronic"), release.getGenres());
        assertEquals(Collections.singletonList("Deep House"), release.getStyles());
        assertEquals("Sweden", release.getCountry());
        assertEquals("1999-03-00", release.getReleased());
        assertEquals(1660109, release.getMaster_id().getId());
        assertEquals(6, release.getTracklist().size());
        assertEquals(Track.builder().position("A").title("Östermalm").duration("4:45").build(), release.getTracklist().get(0));
        assertEquals(5, release.getIdentifiers().size());
        assertEquals(Identifier.builder().description("A-Side Runout").type("Matrix / Runout").value("MPO SK 032 A1").build(), release.getIdentifiers().get(0));
        assertEquals(6, release.getVideos().size());
        Video video = release.getVideos().get(0);
        assertEquals(296, video.getDuration());
        assertTrue(video.isEmbed());
        assertEquals("https://www.youtube.com/watch?v=MpmbntGDyNE", video.getUri());
        assertEquals("The Persuader - Östermalm", video.getTitle());
        assertEquals(2, release.getCompanies().size());
        assertEquals(Company.builder().id(271046L).name("The Globe Studios").catno("").entity_type("23").entity_type_name("Recorded At").resource_url("https://api.discogs.com/labels/271046").build(), release.getCompanies().get(0));
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
        ReleaseUtils releaseUtils = new ReleaseUtils();
        releaseUtils.setTrackSeparator(" - ");
        List<NormalizedTrack> tracks = releaseUtils.normalizedTracks(release);
        assertEquals(23, tracks.size());
        assertEquals("He Ain't Heavy, He's My Brother - Bridge Over Troubled Water", tracks.get(16).getTitle());
    }


    @Test
    public void testMultiDiscPositions() throws JAXBException {
        Release release = release("release-multidisc-positions.xml");
        List<NormalizedTrack> tracks = new ReleaseUtils().normalizedTracks(release);
        assertEquals(51, tracks.size());
        NormalizedTrack track17Disc3 = tracks.get(tracks.size() - 1);
        assertEquals(3, track17Disc3.getPosition().getDisc());
    }

    @Test
    public void testMultiDiscSections() throws JAXBException {
        Release release = release("release-multidisc-sections.xml");
        List<NormalizedTrack> tracks = new ReleaseUtils().normalizedTracks(release);
        assertEquals(44, tracks.size());
    }

    @Test
    public void testSeries() throws JAXBException {
        Release release = release("release-series.xml");
        assertEquals(1, release.getSeries().size());
        assertEquals("Pulp Fusion", release.getSeries().get(0).getName());
        assertEquals(486989, release.getSeries().get(0).getId());
    }


}
