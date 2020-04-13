package org.ruaux.jdiscogs.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public class User extends Resource {
    @Getter
    @Setter
    private String username;
}
