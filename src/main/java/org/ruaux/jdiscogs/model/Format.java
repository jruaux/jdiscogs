package org.ruaux.jdiscogs.model;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class Format {

    private final static String CD = "CD";

    @Getter
    @Setter
    private Integer qty;
    @Getter
    @Setter
    private List<String> descriptions;
    @Getter
    @Setter
    private String name;

    public boolean isCd() {
        return CD.equalsIgnoreCase(name);
    }

}
