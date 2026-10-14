package com.btcassistant.web;

/** Lançada quando um recurso referenciado por id (alerta, decisão, trade, operação) não existe. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
