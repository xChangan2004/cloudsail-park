package com.changan.common.utils;

public class GeoUtils {

    private static final double EARTH_RADIUS = 6371000; // 米

    public static long calcDistance(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double dLat = radLat1 - radLat2;
        double dLng = Math.toRadians(lng1) - Math.toRadians(lng2);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(dLng / 2), 2);
        return Math.round(2 * EARTH_RADIUS * Math.asin(Math.sqrt(a)));
    }
}
