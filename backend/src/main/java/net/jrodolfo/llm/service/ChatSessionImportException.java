package net.jrodolfo.llm.service;

public class ChatSessionImportException extends RuntimeException {

    public ChatSessionImportException(String message) {
        super(message);
    }

    public ChatSessionImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
