package org.leolo.web.nrinfo.util;

public final class CommonUtil {

    public static int randomInt(int min, int max) {
        return (int) (Math.random() * (max - min) + min);
    }

}
