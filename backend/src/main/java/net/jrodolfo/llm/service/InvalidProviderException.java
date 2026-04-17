package net.jrodolfo.llm.service;

public class InvalidProviderException extends RuntimeException {

    public InvalidProviderException(String message) {
        super(message);
    }
}
