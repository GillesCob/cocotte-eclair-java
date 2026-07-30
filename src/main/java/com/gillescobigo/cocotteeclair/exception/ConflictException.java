package com.gillescobigo.cocotteeclair.exception;

// Ex. email déjà utilisé à l'inscription.
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
