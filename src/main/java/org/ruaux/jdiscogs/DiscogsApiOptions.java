package org.ruaux.jdiscogs;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiscogsApiOptions {

    private String token;
    @Builder.Default
    private String userAgent = "jdiscogs.useragent";

}
