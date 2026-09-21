package com.klaus.moply.application.exception;

import java.util.UUID;

public class PrestacaoServicoNotFoundException extends RuntimeException {

    public PrestacaoServicoNotFoundException(UUID id) {
        super("Prestação de serviço não encontrada: " + id);
    }
}