package io.github.glocation87.nature7.map;

import java.nio.file.Path;

// worldData is the folder holding the map's region/ directory
public record GameMap(String id, String gameId, MapInfo info, Object data, Path worldData) {

    public <T> T data(Class<T> schema) {
        return schema.cast(data);
    }
}
