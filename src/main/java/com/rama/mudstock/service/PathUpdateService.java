package com.rama.mudstock.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

@Service
public class PathUpdateService {

    public record PathUpdateResult(int filesScanned,
                                   int filesUpdated,
                                   int replacementsMade,
                                   List<String> updatedFiles,
                                   List<String> skippedFiles) {
    }

    private record ReplaceResult(String text, int count) {
    }

    public PathUpdateResult replaceInFolder(Path folder, String findValue, String replaceValue) {
        int filesScanned = 0;
        int filesUpdated = 0;
        int replacementsMade = 0;
        List<String> updatedFiles = new ArrayList<>();
        List<String> skippedFiles = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(folder)) {
            List<Path> files = walk.filter(Files::isRegularFile).toList();
            for (Path file : files) {
                filesScanned++;
                String original;
                try {
                    original = Files.readString(file, StandardCharsets.UTF_8);
                } catch (Exception ex) {
                    skippedFiles.add(file.toString());
                    continue;
                }

                ReplaceResult replaceResult = applyReplace(original, findValue, replaceValue);
                if (replaceResult.count() <= 0) {
                    continue;
                }

                try {
                    Files.writeString(file, replaceResult.text(), StandardCharsets.UTF_8);
                    filesUpdated++;
                    replacementsMade += replaceResult.count();
                    updatedFiles.add(file.toString());
                } catch (IOException ex) {
                    skippedFiles.add(file.toString());
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to scan folder: " + folder, ex);
        }

        return new PathUpdateResult(filesScanned, filesUpdated, replacementsMade, updatedFiles, skippedFiles);
    }

    private ReplaceResult applyReplace(String text, String findValue, String replaceValue) {
        if (findValue == null || findValue.isEmpty()) {
            return new ReplaceResult(text, 0);
        }

        if (!findValue.contains("*")) {
            int count = countOccurrences(text, findValue);
            if (count == 0) {
                return new ReplaceResult(text, 0);
            }
            return new ReplaceResult(text.replace(findValue, replaceValue), count);
        }

        int wildcardIndex = findValue.indexOf('*');
        String prefix = findValue.substring(0, wildcardIndex);
        String suffix = findValue.substring(wildcardIndex + 1);

        if (prefix.isEmpty() && suffix.isEmpty()) {
            return new ReplaceResult(text, 0);
        }

        StringBuilder out = new StringBuilder(text.length());
        int cursor = 0;
        int count = 0;

        while (cursor < text.length()) {
            int start = text.indexOf(prefix, cursor);
            if (start < 0) {
                out.append(text, cursor, text.length());
                break;
            }

            out.append(text, cursor, start);
            int captureStart = start + prefix.length();

            int end;
            String capture;

            if (suffix.isEmpty()) {
                end = captureStart;
                while (end < text.length() && !isTokenBoundary(text.charAt(end))) {
                    end++;
                }
                capture = text.substring(captureStart, end);
            } else {
                int suffixPos = text.indexOf(suffix, captureStart);
                if (suffixPos < 0) {
                    out.append(text, start, text.length());
                    cursor = text.length();
                    break;
                }
                capture = text.substring(captureStart, suffixPos);
                end = suffixPos + suffix.length();
            }

            String replacement = replaceValue.contains("*") ? replaceValue.replace("*", capture) : replaceValue;
            out.append(replacement);
            cursor = end;
            count++;
        }

        return new ReplaceResult(out.toString(), count);
    }

    private int countOccurrences(String text, String token) {
        int count = 0;
        int from = 0;
        while (true) {
            int idx = text.indexOf(token, from);
            if (idx < 0) {
                return count;
            }
            count++;
            from = idx + token.length();
        }
    }

    private boolean isTokenBoundary(char ch) {
        return Character.isWhitespace(ch)
            || ch == '"'
            || ch == '\''
            || ch == ')'
            || ch == '('
            || ch == ','
            || ch == ';';
    }
}
