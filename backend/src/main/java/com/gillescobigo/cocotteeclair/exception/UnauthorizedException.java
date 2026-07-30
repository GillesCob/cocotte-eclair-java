package com.gillescobigo.cocotteeclair.exception;

// Levée quand un utilisateur authentifié tente d'agir sur une ressource dont il n'est pas propriétaire.
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
