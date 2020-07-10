package org.ruaux.jdiscogs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
