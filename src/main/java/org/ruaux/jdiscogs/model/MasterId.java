package org.ruaux.jdiscogs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.NONE)
public class MasterId {

    @XmlValue
    private Long id;
    @XmlAttribute
    private boolean is_main_release;

    public MasterId(Long id) {
        this.id = id;
    }
}
