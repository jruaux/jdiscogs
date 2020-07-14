package org.ruaux.jdiscogs.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@Data
@XmlAccessorType(XmlAccessType.NONE)
public class MasterId {

    @XmlValue
    private Long id;
    @XmlAttribute
    private boolean is_main_release;

    public MasterId() {
    }

    public MasterId(Long id) {
        this.id = id;
    }
}
