package io.github.glocation87.nature7.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileTreesTest {

    @TempDir
    Path temp;

    @Test
    void copiesNestedFolders() throws IOException {
        Path source = temp.resolve("source");
        Files.createDirectories(source.resolve("region"));
        Files.writeString(source.resolve("region").resolve("r.0.0.mca"), "chunks");
        Files.writeString(source.resolve("level.dat"), "level");

        Path target = temp.resolve("target");
        FileTrees.copy(source, target);

        assertEquals("chunks", Files.readString(target.resolve("region").resolve("r.0.0.mca")));
        assertEquals("level", Files.readString(target.resolve("level.dat")));
    }

    @Test
    void deletesNestedFolders() throws IOException {
        Path root = temp.resolve("world");
        Files.createDirectories(root.resolve("a").resolve("b"));
        Files.writeString(root.resolve("a").resolve("b").resolve("file"), "x");

        FileTrees.delete(root);

        assertFalse(Files.exists(root));
    }

    @Test
    void deletingSomethingMissingIsFine() throws IOException {
        FileTrees.delete(temp.resolve("missing"));
    }
}
