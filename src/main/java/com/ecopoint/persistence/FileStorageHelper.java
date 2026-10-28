package com.ecopoint.persistence;

import com.ecopoint.exceptions.PersistenceException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that encapsulates low-level flat-file reading and writing
 * operations in UTF-8 encoding, converting any IOException into a PersistenceException.
 *
 * @author mlanderoc
 * @version 1.0
 */
public final class FileStorageHelper {

    public static final String DEFAULT_DATA_DIR = "data";
    public static final String FIELD_SEPARATOR = "|";
    public static final String FIELD_SPLIT_REGEX = "\\|";

    private FileStorageHelper() {
    }

    /**
     * Resolves a file path inside the data directory and ensures the directory exists.
     *
     * @param fileName name of the flat file
     * @return resolved file path
     */
    public static Path resolveDataFile(String fileName) {
        try {
            Path dir = Paths.get(DEFAULT_DATA_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path file = dir.resolve(fileName);
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
            return file;
        } catch (IOException ex) {
            throw new PersistenceException("Inicialización del archivo de datos " + fileName, ex);
        }
    }

    /**
     * Reads all non-empty lines from the specified flat file.
     *
     * @param fileName name of the flat file inside the data directory
     * @return list of non-empty lines
     */
    public static List<String> readLines(String fileName) {
        Path file = resolveDataFile(fileName);
        try {
            List<String> allLines = Files.readAllLines(file, StandardCharsets.UTF_8);
            List<String> nonEmpty = new ArrayList<>();
            for (String line : allLines) {
                if (line != null && !line.trim().isEmpty()) {
                    nonEmpty.add(line);
                }
            }
            return nonEmpty;
        } catch (IOException ex) {
            throw new PersistenceException("Lectura del archivo plano " + fileName, ex);
        }
    }

    /**
     * Overwrites the specified flat file with the provided lines in UTF-8.
     *
     * @param fileName name of the flat file inside the data directory
     * @param lines    lines to write
     */
    public static void writeLines(String fileName, List<String> lines) {
        Path file = resolveDataFile(fileName);
        try {
            Files.write(file, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new PersistenceException("Escritura del archivo plano " + fileName, ex);
        }
    }

    /**
     * Sanitizes a string value before writing to a pipe-delimited flat file.
     *
     * @param text input string
     * @return sanitized single-line string without pipe characters
     */
    public static String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("|", "/").replace("\r", " ").replace("\n", " ").trim();
    }
}
