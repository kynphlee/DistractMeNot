package com.ndl.distractmenot.util;

import android.location.Location;

/**
 * Created by kynphlee on 1/19/17.
 */
public class DMNUtils {
    public static int speedToMPH(Location location) {
        return Math.round((location.getSpeed() * 2.23694f));
    }

    public static int speedToMPH(float speed) {
        return Math.round((speed * 2.23694f));
    }

    public static double longToDecimal(long longVal) {
        return Long.valueOf(longVal).doubleValue();
    }
}
