package org.ruaux.jdiscogs.model;

import lombok.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
