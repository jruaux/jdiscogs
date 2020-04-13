package org.ruaux.jdiscogs.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public class Image extends Resource {

    public static final String TYPE_PRIMARY = "primary";

    @Getter
    @Setter
    private String uri;
    @Getter
    @Setter
    private Integer height;
    @Getter
    @Setter
    private Integer width;
    @Getter
    @Setter
    private String type;
    @Getter
    @Setter
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