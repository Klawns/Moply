package com.klaus.moply.infra.web.dto.response;

import java.math.BigDecimal;

public record PrestacaoServicoCalculos(BigDecimal valorTotal,
        BigDecimal horasIndividuais, BigDecimal valorIndividual) {

}
