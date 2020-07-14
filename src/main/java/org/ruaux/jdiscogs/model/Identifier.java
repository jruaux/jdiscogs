package org.ruaux.jdiscogs.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlRootElement(name = "identifier")
@XmlAccessorType(XmlAccessType.FIELD)
public class Identifier {

    @XmlAttribute
    private String description;
    @XmlAttribute
    private String type;
    @XmlAttribute
    private String value;
}
