package org.ruaux.jdiscogs.data;

import com.redislabs.lettusearch.search.Document;
import org.ruaux.jdiscogs.JDiscogsProperties;
import org.ruaux.jdiscogs.model.Artist;
import org.ruaux.jdiscogs.model.Image;
import org.ruaux.jdiscogs.model.Master;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.ruaux.jdiscogs.data.Fields.*;

@Component
public class MasterProcessor implements ItemProcessor<Master, Document<String, String>> {

    private final JDiscogsProperties props;
    private final XmlCodec codec;

    public MasterProcessor(JDiscogsProperties props, XmlCodec codec) {
        this.props = props;
        this.codec = codec;
    }


    @Override
    public Document<String, String> process(Master master) throws Exception {
        if (!hasImage(master) || master.getYear() == null) {
            return null;
        }
        Document<String, String> doc = Document.<String, String>builder().id(master.getIdString()).score(1d).build();
        if (master.getArtists() != null && !master.getArtists().isEmpty()) {
            Artist artist = master.getArtists().get(0);
            if (artist != null) {
                doc.put(ARTIST, artist.getName());
                doc.put(ARTIST_ID, String.valueOf(artist.getId()));
            }
        }
        Set<String> genres = new LinkedHashSet<>();
        genres.addAll(master.getGenres());
        genres.addAll(master.getStyles());
        doc.put(GENRES, String.join(props.getHashArrayDelimiter(), sanitize(genres)));
        doc.put(TITLE, master.getTitle());
        doc.put(YEAR, String.valueOf(master.getYear()));
        doc.setPayload(codec.getXml(master));
        return doc;
    }

    private boolean hasImage(Master master) {
        Image image = master.getPrimaryImage();
        if (image == null) {
            return false;
        }
        return image.getHeight() >= props.getMinImageHeight() && image.getWidth() >= props.getMinImageWidth() && Math.abs(1 - image.getRatio()) <= props.getImageRatioTolerance();
    }

    private List<String> sanitize(Set<String> getGenres) {
        List<String> result = new ArrayList<>();
        getGenres.forEach(genre -> result.add(genre.replace(',', ' ')));
        return result;
    }

}
