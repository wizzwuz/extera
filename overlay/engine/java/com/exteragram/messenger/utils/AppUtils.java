package com.exteragram.messenger.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class AppUtils {
    private static volatile Object gson;

    public static Object getGson() {
        Object value = gson;
        if (value == null) {
            try {
                value = Class.forName("com.google.gson.Gson").getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            gson = value;
        }
        return value;
    }

    public static String stackTraceToString(Throwable t) {
        if (t == null) return "";
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static boolean compareVersions(String operator, String version1, String version2) {
        int cmp = compareVersionValues(version1, version2);
        return switch (operator) {
            case "==" -> cmp == 0;
            case ">" -> cmp > 0;
            case "<" -> cmp < 0;
            case ">=" -> cmp >= 0;
            case "<=" -> cmp <= 0;
            default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
        };
    }

    public static boolean compareVersions(String operator, int version1, int version2) {
        int cmp = Integer.compare(version1, version2);
        return switch (operator) {
            case "==" -> cmp == 0;
            case ">" -> cmp > 0;
            case "<" -> cmp < 0;
            case ">=" -> cmp >= 0;
            case "<=" -> cmp <= 0;
            default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
        };
    }

    public static int compareVersionValues(String version1, String version2) {
        String[] parts1 = (version1 != null ? version1 : "0").split("\\.");
        String[] parts2 = (version2 != null ? version2 : "0").split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int p1 = i < parts1.length ? parsePart(parts1[i]) : 0;
            int p2 = i < parts2.length ? parsePart(parts2[i]) : 0;
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
        }
        return 0;
    }

    private static int parsePart(String part) {
        try {
            String digits = part.replaceAll("[^0-9].*$", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    public static void log(Throwable t) {
        org.telegram.messenger.FileLog.e(t);
    }

    public static void log(Object o) {
        org.telegram.messenger.FileLog.d(String.valueOf(o));
    }
}
