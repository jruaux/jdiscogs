package org.ruaux.jdiscogs.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlRootElement(name = "image")
@XmlAccessorType(XmlAccessType.FIELD)
public class Image {

    @XmlAttribute
    private Integer height;
    @XmlAttribute
    private String type;
    @XmlAttribute
    private Integer width;
    @XmlAttribute
    private String uri;
    @XmlAttribute
    private String uri150;

}