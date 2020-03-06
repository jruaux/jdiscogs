package org.ruaux.jdiscogs.data;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.ruaux.jdiscogs.data.xml.Release;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class ReleaseCodec implements InitializingBean {

	private JAXBContext context;
	private Marshaller marshaller;
	private Unmarshaller unmarshaller;

	@Override
	public void afterPropertiesSet() throws Exception {
		this.context = JAXBContext.newInstance(Release.class);
		this.unmarshaller = context.createUnmarshaller();
		this.marshaller = context.createMarshaller();
	}

	public String xml(Release release) throws JAXBException {
		StringWriter writer = new StringWriter();
		marshaller.marshal(release, writer);
		return writer.toString();
	}

	public Release release(String xml) throws JAXBException {
		return (Release) unmarshaller.unmarshal(new StringReader(xml));
	}

}
