package org.ruaux.jdiscogs.data;

import com.redislabs.lettusearch.search.Document;
import lombok.extern.slf4j.Slf4j;
import org.ruaux.jdiscogs.JDiscogsProperties;
import org.ruaux.jdiscogs.model.Artist;
import org.ruaux.jdiscogs.model.Release;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.stream.Collectors;

import static org.ruaux.jdiscogs.data.Fields.*;

@Slf4j
@Component
@Scope(scopeName = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ReleaseProcessor implements ItemProcessor<Release, Document<String, String>>, InitializingBean {

    private final JDiscogsProperties props;
    private final TextSanitizer sanitizer;
    private final Jaxb2Marshaller jaxb2Marshaller;
    private final ThreadLocal<Marshaller> marshaller = new ThreadLocal<>();

    public ReleaseProcessor(JDiscogsProperties props, TextSanitizer sanitizer, Jaxb2Marshaller marshaller) {
        this.props = props;
        this.sanitizer = sanitizer;
        this.jaxb2Marshaller = marshaller;
    }

    @Override
    public void afterPropertiesSet() {
        this.marshaller.set(jaxb2Marshaller.createMarshaller());
    }

    @Override
    public Document<String, String> process(Release release) throws JAXBException {
        Document<String, String> doc = Document.builder().id(String.valueOf(release.getId())).score(1d).build();
        doc.put(ARTIST, sanitizer.sanitize(release.getArtists().stream().map(Artist::getName).collect(Collectors.joining(props.getData().getSeparator()))));
        doc.put(TITLE, sanitizer.sanitize(release.getTitle()));
        doc.put(ID, String.valueOf(release.getId()));
        StringWriter writer = new StringWriter();
        marshaller.get().marshal(release, writer);
        doc.setPayload(writer.toString());
        return doc;
    }

}
