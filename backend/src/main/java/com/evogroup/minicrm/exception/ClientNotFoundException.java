package com.evogroup.minicrm.exception;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(Long id) {
        super("Client not found: " + id);
    }
}
