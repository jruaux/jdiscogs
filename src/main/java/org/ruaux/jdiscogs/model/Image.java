package org.ruaux.jdiscogs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@XmlRootElement(name = "image")
@XmlAccessorType(XmlAccessType.FIELD)
public class Image {

    public static final String TYPE_PRIMARY = "primary";

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

    public boolean isPrimary() {
        return TYPE_PRIMARY.equals(type);
    }

    public Double getRatio() {
        if (height == null) {
            return null;
        }
        if (width == null) {
            return null;
        }
        return height.doubleValue() / width.doubleValue();
    }
}