package org.ruaux.jdiscogs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "format")
@XmlAccessorType(XmlAccessType.FIELD)
public class Format {

    public final static String CD = "CD";
    public final static String VINYL = "Vinyl";

    @XmlAttribute
    private String name;
    @XmlAttribute
    private Integer qty;
    @XmlAttribute
    private String text;
    @XmlElement(name="description")
    @XmlElementWrapper(name="descriptions")
    private List<String> descriptions;

    public boolean isCd() {
        return CD.equalsIgnoreCase(name);
    }

    public boolean isVinyl() {
        return VINYL.equalsIgnoreCase(name);
    }

}
