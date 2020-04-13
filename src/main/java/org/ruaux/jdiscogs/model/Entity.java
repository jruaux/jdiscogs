package org.ruaux.jdiscogs.model;

import lombok.Getter;
import lombok.Setter;

public class Entity extends IdResource {
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String entity_type;
    @Getter
    @Setter
    private String entity_type_name;
    @Getter
    @Setter
    private String catno;

}
