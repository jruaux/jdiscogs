package org.ruaux.jdiscogs.model;

import lombok.Getter;
import lombok.Setter;

public class IdResource extends Resource {
    @Getter
    @Setter
    private Long id;

    public String getIdString() {
        return String.valueOf(id);
    }
}
