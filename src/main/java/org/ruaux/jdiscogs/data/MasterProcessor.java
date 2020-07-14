package org.ruaux.jdiscogs.data;

import com.redislabs.lettusearch.search.Document;
import org.ruaux.jdiscogs.JDiscogsProperties;
import org.ruaux.jdiscogs.ReleaseUtils;
import org.ruaux.jdiscogs.model.Artist;
import org.ruaux.jdiscogs.model.Image;
import org.ruaux.jdiscogs.model.Master;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.ruaux.jdiscogs.data.Fields.*;

@Component
@Scope(scopeName = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MasterProcessor implements ItemProcessor<Master, Document<String, String>>, InitializingBean {

    private final JDiscogsProperties props;
    private final Jaxb2Marshaller jaxb2Marshaller;
    private final ThreadLocal<Marshaller> marshaller = new ThreadLocal<>();

    public MasterProcessor(JDiscogsProperties props, Jaxb2Marshaller jaxb2Marshaller) {
        this.props = props;
        this.jaxb2Marshaller = jaxb2Marshaller;
    }

    @Override
    public void afterPropertiesSet() {
        this.marshaller.set(jaxb2Marshaller.createMarshaller());
    }

    @Override
    public Document<String, String> process(Master master) throws JAXBException {
        if (!hasImage(master) || master.getYear() == null) {
            return null;
        }
        Document<String, String> doc = Document.builder().id(String.valueOf(master.getId())).score(1d).build();
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
        doc.put(GENRES, String.join(props.getData().getSeparator(), sanitize(genres)));
        doc.put(TITLE, master.getTitle());
        doc.put(YEAR, String.valueOf(master.getYear()));
        StringWriter writer = new StringWriter();
        marshaller.get().marshal(master, writer);
        doc.setPayload(writer.toString());
        return doc;
    }

    private boolean hasImage(Master master) {
        Image image = ReleaseUtils.primaryImage(master);
        if (image == null) {
            return false;
        }
        return image.getHeight() >= props.getData().getMasters().getMinImageHeight() && image.getWidth() >= props.getData().getMasters().getMinImageWidth() && Math.abs(1 - ReleaseUtils.ratio(image)) <= props.getData().getMasters().getImageRatioTolerance();
    }

    private List<String> sanitize(Set<String> getGenres) {
        List<String> result = new ArrayList<>();
        getGenres.forEach(genre -> result.add(genre.replace(',', ' ')));
        return result;
    }

}
