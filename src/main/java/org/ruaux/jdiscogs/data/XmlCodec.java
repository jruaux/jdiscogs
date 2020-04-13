package org.ruaux.jdiscogs.data;

import org.ruaux.jdiscogs.model.Master;
import org.ruaux.jdiscogs.model.Release;
import org.springframework.oxm.xstream.XStreamMarshaller;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

@Component
public class XmlCodec {

    private final XStreamMarshaller marshaller;

    public XmlCodec(XStreamMarshaller marshaller) {
        this.marshaller = marshaller;
    }

    public String getXml(Master master) throws IOException {
        StringWriter writer = new StringWriter();
        marshaller.marshalWriter(master, writer);
        return writer.toString();
    }

    public String getXml(Release release) throws IOException {
        StringWriter writer = new StringWriter();
        marshaller.marshalWriter(release, writer);
        return writer.toString();
    }

    public Master getMaster(String xml) throws IOException {
        return (Master) marshaller.unmarshalReader(new StringReader(xml));
    }

    public Release getRelease(String xml) throws IOException {
        return (Release) marshaller.unmarshalReader(new StringReader(xml));
    }

}
