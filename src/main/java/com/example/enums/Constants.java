package com.example.enums;

public final class Constants {
    private Constants() {}

    // Limits
    public static final int MAX_FILES = 10;
    public static final int SESSION_TTL_MINUTES = 30;
    public static final int TEXT_THRESHOLD_MAX = 50; // <=50 -> text, >=51 -> Excel

    // Callback data
    public static final String CB_EXPORT_HELP = "EXPORT_HELP";
    public static final String CB_FAQ = "FAQ";
    public static final String CB_START_PROCESS = "START_PROCESS";
    public static final String CB_PROCESS_OTHER = "PROCESS_OTHER";
    public static final String CB_RESTART = "RESTART";
}
