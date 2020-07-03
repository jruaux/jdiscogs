package org.ruaux.jdiscogs.model;

import lombok.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class Label {

    @XmlAttribute
    private Long id;
    @XmlAttribute
    private String name;
    @XmlAttribute
    private String catno;

}
