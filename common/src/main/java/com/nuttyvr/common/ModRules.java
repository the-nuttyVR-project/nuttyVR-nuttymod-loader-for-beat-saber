package com.nuttyvr.common;

public final class ModRules {
    private ModRules() {}
    public static String text(String value, int max, String field) {
        if (value == null || value.trim().isEmpty() || value.length() > max) throw new IllegalArgumentException("Invalid " + field);
        for (int i = 0; i < value.length(); i++) if (Character.isISOControl(value.charAt(i))) throw new IllegalArgumentException("Invalid " + field);
        return value;
    }
    public static String id(String value) {
        if (value == null || !value.matches("[a-z][a-z0-9_.-]{2,63}")) throw new IllegalArgumentException("Invalid mod ID");
        return value;
    }
    public static String target(String value) {
        if (value == null || value.length() > 160 || !value.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+")) throw new IllegalArgumentException("Invalid game package");
        return value;
    }
    public static String version(String value) {
        if (value == null || !value.matches("[0-9][a-zA-Z0-9.+_-]{0,31}")) throw new IllegalArgumentException("Invalid version; exact versions are required");
        return value;
    }
    public static String color(String value) {
        if (value == null || !value.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Color must be #RRGGBB");
        return value;
    }
    public static double speed(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.5 || value > 2.0) throw new IllegalArgumentException("Speed must be between 0.5 and 2.0");
        return value;
    }
    public static boolean compatible(String target, String targetVersion, String game, String gameVersion) {
        return target.equals(game) && targetVersion.equals(gameVersion);
    }
}
