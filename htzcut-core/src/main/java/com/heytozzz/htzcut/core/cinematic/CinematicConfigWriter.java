package com.heytozzz.htzcut.core.cinematic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serializes a CinematicDefinition back to YAML - hand-written for the
 * same reasons as EventConfigWriter: small, fixed schema, exact
 * predictable output.
 */
public class CinematicConfigWriter {

    public void write(CinematicDefinition def, Path targetFile) throws IOException {
        Files.writeString(targetFile, toYaml(def));
    }

    public String toYaml(CinematicDefinition def) {
        StringBuilder sb = new StringBuilder();

        sb.append("name: \"").append(escape(def.getName())).append("\"\n");

        if (def.getDurationSeconds() != null) {
            sb.append("duration_seconds: ").append(def.getDurationSeconds()).append("\n");
        }

        sb.append("keyframes:\n");
        for (KeyframeConfig keyframe : def.getKeyframes()) {
            sb.append("  - x: ").append(keyframe.getX()).append("\n");
            sb.append("    y: ").append(keyframe.getY()).append("\n");
            sb.append("    z: ").append(keyframe.getZ()).append("\n");
            sb.append("    yaw: ").append(keyframe.getYaw()).append("\n");
            sb.append("    pitch: ").append(keyframe.getPitch()).append("\n");
            if (keyframe.getTimeSeconds() != null) {
                sb.append("    time_seconds: ").append(keyframe.getTimeSeconds()).append("\n");
            }
        }

        return sb.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
