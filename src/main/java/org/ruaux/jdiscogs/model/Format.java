package org.ruaux.jdiscogs.model;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.List;

@Data
@XmlRootElement(name = "format")
@XmlAccessorType(XmlAccessType.FIELD)
public class Format {

    @XmlAttribute
    private String name;
    @XmlAttribute
    private Integer qty;
    @XmlAttribute
    private String text;
    @XmlElement(name="description")
    @XmlElementWrapper(name="descriptions")
    private List<String> descriptions;

}
