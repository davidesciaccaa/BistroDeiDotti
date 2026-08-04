package com.angolodivino.menu;

public class MenuTranslationException extends RuntimeException {
    private final String code;
    public MenuTranslationException(String code, String message) { super(message); this.code = code; }
    public MenuTranslationException(String code, String message, Throwable cause) { super(message, cause); this.code = code; }
    public String code() { return code; }
}
