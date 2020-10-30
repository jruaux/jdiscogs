package org.ruaux.jdiscogs;

import org.ruaux.jdiscogs.model.Image;
import org.ruaux.jdiscogs.model.Master;
import org.ruaux.jdiscogs.model.Release;

import java.util.List;

public class Utils {

    private static final String TYPE_PRIMARY = "primary";

    public static boolean isPrimary(Image image) {
        return TYPE_PRIMARY.equals(image.getType());
    }

    public static Image primaryImage(List<Image> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream().filter(Utils::isPrimary).findFirst().orElseGet(() -> images.get(0));
    }

    public static Image primaryImage(Release release) {
        return primaryImage(release.getImages());
    }

    public static Image primaryImage(Master master) {
        return primaryImage(master.getImages());
    }
}
