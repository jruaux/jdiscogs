package org.ruaux.jdiscogs.model;

import lombok.Getter;
import lombok.Setter;

public class Identifier {
    @Getter
    @Setter
    private String type;
    @Getter
    @Setter
    private String description;
    @Getter
    @Setter
    private String value;
}
