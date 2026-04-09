package com.crown.billing.constants;

public enum UsageType {
    SHORTS("shorts_count"),
    LONGFORM("longform_count"),
    VOICE_CLONE("voice_clone_count"),
    AI_REWRITE("ai_rewrite_count");

    private final String column;

    UsageType(String column) {
        this.column = column;
    }

    public String getColumn() {
        return column;
    }
}
