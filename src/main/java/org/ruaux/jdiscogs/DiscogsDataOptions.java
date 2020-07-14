package org.ruaux.jdiscogs;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiscogsDataOptions {

    @Builder.Default
    private String separator = ",";
    @Builder.Default
    private int batch = 50;
    @Builder.Default
    private int threads = 1;
    private boolean noOp;
    @Builder.Default
    private MasterImportOptions masters = MasterImportOptions.builder().build();
    @Builder.Default
    private ReleaseImportOptions releases = ReleaseImportOptions.builder().build();

    @Data
    @Builder
    public static class MasterImportOptions {

        public static final String DEFAULT_INDEX = "masters";

        @Builder.Default
        private String url = "https://discogs-data.s3-us-west-2.amazonaws.com/data/2020/discogs_20200703_masters.xml.gz";
        @Builder.Default
        private String index = DEFAULT_INDEX;
        @Builder.Default
        private int minImageHeight = 400;
        @Builder.Default
        private int minImageWidth = 400;
        @Builder.Default
        private double imageRatioTolerance = .05;

    }

    @Data
    @Builder
    public static class ReleaseImportOptions {

        public static final String DEFAULT_INDEX = "releases";

        @Builder.Default
        private String url = "https://discogs-data.s3-us-west-2.amazonaws.com/data/2020/discogs_20200703_releases.xml.gz";
        @Builder.Default
        private String index = DEFAULT_INDEX;

    }

}
