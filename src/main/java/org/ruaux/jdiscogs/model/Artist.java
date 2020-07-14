package org.ruaux.jdiscogs.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "artist")
public class Artist {

    private Long id;
    private String resource_url;
    private String join;
    private String name;
    private String anv;
    private String tracks;
    private String role;

}