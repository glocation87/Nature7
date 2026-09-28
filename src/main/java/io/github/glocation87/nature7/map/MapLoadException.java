package io.github.glocation87.nature7.map;

// checked on purpose, a broken map.yml is bad input the caller has to handle, not a bug
public final class MapLoadException extends Exception {
    private static final long serialVersionUID = 1L;

    public MapLoadException(String message) {
        super(message);
    }

    public MapLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
