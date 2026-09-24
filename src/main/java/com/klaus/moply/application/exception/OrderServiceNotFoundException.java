package com.klaus.moply.application.exception;

import java.util.UUID;

public class OrderServiceNotFoundException extends RuntimeException {

    public OrderServiceNotFoundException(UUID id) {
        super("Prestação de serviço não encontrada: " + id);
    }
}