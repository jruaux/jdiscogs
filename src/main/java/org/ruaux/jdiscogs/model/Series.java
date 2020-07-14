package org.ruaux.jdiscogs.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlRootElement(name = "series")
@XmlAccessorType(XmlAccessType.FIELD)
public class Series {

    private Long id;
    private String name;
    private String catno;
    private String entity_type;
    private String entity_type_name;
    private String resource_url;

}
