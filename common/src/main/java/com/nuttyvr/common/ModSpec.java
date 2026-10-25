package com.nuttyvr.common;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Arrays;
import java.util.HashSet;
import org.json.JSONObject;

public final class ModSpec {
    public static final int MAX_BYTES = 65536;
    public final String id, name, version, target, targetVersion, color;
    public final double speed;
    public ModSpec(String data) throws Exception {
        if (data.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw new IllegalArgumentException("Profile exceeds 64 KiB");
        JSONObject json = new JSONObject(data);
        HashSet<String> keys = new HashSet<>(Arrays.asList("schema", "id", "name", "version", "targetPackage", "targetVersion", "color", "speed"));
        Iterator<String> iterator = json.keys();
        while (iterator.hasNext()) if (!keys.contains(iterator.next())) throw new IllegalArgumentException("Unknown profile field");
        if (!"nuttymod/1".equals(json.getString("schema"))) throw new IllegalArgumentException("Expected nuttymod/1 schema");
        id = ModRules.id(string(json, "id"));
        name = ModRules.text(string(json, "name"), 64, "name");
        version = ModRules.version(string(json, "version"));
        target = ModRules.target(string(json, "targetPackage"));
        targetVersion = ModRules.version(string(json, "targetVersion"));
        color = ModRules.color(string(json, "color"));
        Object value = json.get("speed");
        if (!(value instanceof Number)) throw new IllegalArgumentException("Speed must be a number");
        speed = ModRules.speed(((Number) value).doubleValue());
    }
    private static String string(JSONObject json, String key) throws Exception {
        Object value = json.get(key);
        if (!(value instanceof String)) throw new IllegalArgumentException(key + " must be text");
        return (String) value;
    }
    public String json() throws Exception {
        return new JSONObject().put("schema", "nuttymod/1").put("id", id).put("name", name).put("version", version)
            .put("targetPackage", target).put("targetVersion", targetVersion).put("color", color).put("speed", speed).toString();
    }
    public static String example() {
        return "{\"schema\":\"nuttymod/1\",\"id\":\"mint-rush\",\"name\":\"Mint Rush\",\"version\":\"1.0.0\",\"targetPackage\":\"com.nuttyvr.demo\",\"targetVersion\":\"0.1.0\",\"color\":\"#A9EF64\",\"speed\":1.5}";
    }
}
