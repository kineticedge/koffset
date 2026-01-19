package io.kineticedge.koffset.util;

public final class StringUtil {

    private StringUtil() {
    }

    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
}
