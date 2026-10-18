package io.github.xiangwenquan830.silversamurai;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.joml.Vector3f;

/** Holds only geometry and one JSON keyframe at a time while constructing runtime tracks. */
final class RuntimeRigReader {
    record Data(JsonObject geometry, List<SamuraiRig.Clip> clips) {}

    static Data read(int phase) {
        String path = "/assets/silver_samurai/models/phase" + phase + ".bbmodel";
        try (InputStream in = RuntimeRigReader.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("Missing rig: " + path);
            try (JsonReader reader = new JsonReader(new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)))) {
                return read(reader);
            }
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("Unable to load Silver Samurai rig " + phase, ex);
        }
    }

    static Data read(JsonReader reader) throws IOException {
        JsonObject geometry = new JsonObject();
        List<SamuraiRig.Clip> clips = new ArrayList<>();
        boolean animations = false;
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "groups" -> geometry.add("groups", JsonParser.parseReader(reader));
                case "elements" -> geometry.add("elements", JsonParser.parseReader(reader));
                case "outliner" -> geometry.add("outliner", JsonParser.parseReader(reader));
                case "animations" -> {
                    animations = true;
                    reader.beginArray();
                    while (reader.hasNext()) clips.add(clip(reader));
                    reader.endArray();
                }
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        if (!animations || !geometry.has("groups") || !geometry.has("elements")
                || !geometry.has("outliner")) throw new IOException("Incomplete rig resource");
        return new Data(geometry, clips);
    }

    private static SamuraiRig.Clip clip(JsonReader reader) throws IOException {
        String name = null;
        Float length = null;
        Boolean loop = null;
        Map<String, SamuraiRig.Track> tracks = new HashMap<>();
        boolean animators = false;
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "name" -> name = reader.nextString();
                case "length" -> length = Float.parseFloat(reader.nextString());
                case "loop" -> loop = "loop".equals(reader.nextString());
                case "animators" -> {
                    animators = true;
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String id = reader.nextName();
                        SamuraiRig.Track track = track(reader);
                        if (track != null) tracks.put(id, track);
                    }
                    reader.endObject();
                }
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        if (name == null || length == null || loop == null || !animators)
            throw new IOException("Incomplete animation metadata");
        return new SamuraiRig.Clip(name, length, loop, tracks);
    }

    private static SamuraiRig.Track track(JsonReader reader) throws IOException {
        SamuraiRig.Track track = new SamuraiRig.Track();
        boolean keys = false;
        reader.beginObject();
        while (reader.hasNext()) {
            if (!"keyframes".equals(reader.nextName())) {
                reader.skipValue();
                continue;
            }
            keys = true;
            reader.beginArray();
            while (reader.hasNext()) {
                JsonObject key = JsonParser.parseReader(reader).getAsJsonObject();
                String channel = key.get("channel").getAsString();
                if (!channel.equals("rotation") && !channel.equals("position")
                        && !channel.equals("scale")) continue;
                JsonObject value = key.getAsJsonArray("data_points").get(0).getAsJsonObject();
                track.channel(channel).add(new SamuraiRig.Key(
                        key.get("time").getAsFloat(),
                        new Vector3f(value.get("x").getAsFloat(), value.get("y").getAsFloat(),
                                value.get("z").getAsFloat()),
                        key.has("interpolation") && "step".equals(key.get("interpolation").getAsString())));
            }
            reader.endArray();
        }
        reader.endObject();
        for (var channel : List.of(track.position, track.rotation, track.scale))
            channel.sort(Comparator.comparingDouble(SamuraiRig.Key::time));
        return keys ? track : null;
    }

    static Data reference(JsonObject root) {
        List<SamuraiRig.Clip> clips = new ArrayList<>();
        for (var value : root.getAsJsonArray("animations"))
            clips.add(new SamuraiRig.Clip(value.getAsJsonObject()));
        return new Data(root, clips);
    }

    private RuntimeRigReader() {}
}
