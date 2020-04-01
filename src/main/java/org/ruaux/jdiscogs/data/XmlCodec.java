package org.ruaux.jdiscogs.data;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.ruaux.jdiscogs.data.model.Master;
import org.ruaux.jdiscogs.data.model.Release;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class XmlCodec implements InitializingBean {

	private Marshaller marshaller;
	private Unmarshaller unmarshaller;

	@Override
	public void afterPropertiesSet() throws Exception {
		JAXBContext context = JAXBContext.newInstance(Release.class, Master.class);
		this.unmarshaller = context.createUnmarshaller();
		this.marshaller = context.createMarshaller();
	}

	public String getXml(Master master) throws JAXBException {
		StringWriter writer = new StringWriter();
		marshaller.marshal(master, writer);
		return writer.toString();
	}

	public String getXml(Release release) throws JAXBException {
		StringWriter writer = new StringWriter();
		marshaller.marshal(release, writer);
		return writer.toString();
	}

	public Master getMaster(String xml) throws JAXBException {
		return (Master) unmarshaller.unmarshal(new StringReader(xml));
	}

	public Release getRelease(String xml) throws JAXBException {
		return (Release) unmarshaller.unmarshal(new StringReader(xml));
	}

}
