package org.ruaux.jdiscogs.data;

import com.redislabs.lettusearch.search.Document;
import org.ruaux.jdiscogs.model.Artist;
import org.ruaux.jdiscogs.model.Release;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.ruaux.jdiscogs.data.Fields.*;

@Component
public class ReleaseProcessor implements ItemProcessor<Release, Document<String, String>> {

    private final TextSanitizer sanitizer;
    private final XmlCodec codec;

    public ReleaseProcessor(TextSanitizer sanitizer, XmlCodec codec) {
        this.codec = codec;
        this.sanitizer = sanitizer;
    }

    @Override
    public Document<String, String> process(Release release) throws IOException {
        Document<String, String> doc = Document.<String, String>builder().id(release.getIdString()).score(1d).build();
        Stream<String> artists = release.getArtists().stream().map(Artist::getName);
        doc.put(ARTIST, sanitizer.sanitize(String.join(" ", artists.collect(Collectors.toList()))));
        doc.put(TITLE, sanitizer.sanitize(release.getTitle()));
        doc.put(ID, release.getIdString());
        doc.setPayload(codec.getXml(release));
        return doc;
    }

}
