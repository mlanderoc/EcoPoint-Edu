package com.ecopoint.model;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks that no message of the application shows a rule code (spec 003,
 * R-134): the codes stay in comments, JavaDoc and test names only.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class UserMessagesTest {

    private static final Pattern STRING_LITERAL = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"");
    private static final Pattern RULE_CODE = Pattern.compile("\\b(RN|R|CA)-\\d{3}\\b");

    @Test
    void r134_messagesDoNotShowRuleCodes() throws IOException {
        List<String> found;
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            found = files.filter(file -> file.toString().endsWith(".java"))
                    .flatMap(UserMessagesTest::codesInLiterals)
                    .toList();
        }

        assertEquals(List.of(), found);
    }

    private static Stream<String> codesInLiterals(Path file) {
        try {
            return Files.readAllLines(file).stream()
                    .filter(line -> !line.trim().startsWith("*") && !line.trim().startsWith("//"))
                    .flatMap(line -> {
                        Matcher literal = STRING_LITERAL.matcher(line);
                        Stream.Builder<String> codes = Stream.builder();
                        while (literal.find()) {
                            if (RULE_CODE.matcher(literal.group()).find()) {
                                codes.add(file.getFileName() + ": " + literal.group());
                            }
                        }
                        return codes.build();
                    });
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + file, e);
        }
    }
}
