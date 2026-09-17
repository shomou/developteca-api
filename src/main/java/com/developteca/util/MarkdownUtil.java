package com.developteca.util;

public class MarkdownUtil {

    private MarkdownUtil() {}

    // Aproximación por regex, suficiente para un excerpt: no pretende parsear Markdown completo.
    public static String toPlainText(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        String text = markdown;

        text = text.replaceAll("(?s)```.*?```", " ");
        text = text.replaceAll("`([^`]*)`", "$1");
        text = text.replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", " ");
        text = text.replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1");
        text = text.replaceAll("(?m)^#{1,6}\\s+", "");
        text = text.replaceAll("(?m)^\\s*[-*+]\\s+", "");
        text = text.replaceAll("(?m)^\\s*\\d+\\.\\s+", "");
        text = text.replaceAll("(?m)^>\\s?", "");
        text = text.replaceAll("(?m)^[-*_]{3,}\\s*$", "");
        text = text.replaceAll("(\\*\\*|__)(.+?)\\1", "$2");
        text = text.replaceAll("(\\*|_)(.+?)\\1", "$2");
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }
}
