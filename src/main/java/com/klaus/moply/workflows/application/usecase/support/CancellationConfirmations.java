package com.klaus.moply.workflows.application.usecase.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.klaus.moply.shared.application.usecase.exception.ApplicationException;
import com.klaus.moply.workflows.application.usecase.dto.PaymentConfirmation;

public final class CancellationConfirmations {

	private final Map<UUID, PaymentConfirmation> byPayment;

	private CancellationConfirmations(Map<UUID, PaymentConfirmation> byPayment) {
		this.byPayment = Map.copyOf(byPayment);
	}

	public Map<UUID, PaymentConfirmation> byPayment() {
		return byPayment;
	}

	public static CancellationConfirmations from(List<PaymentConfirmation> confirmations) {
		var byPayment = new HashMap<UUID, PaymentConfirmation>();
		for (var confirmation : confirmations) {
			if (confirmation == null || confirmation.paymentId() == null || !confirmation.confirmNoMoneyReceived()
					|| confirmation.reason() == null || confirmation.reason().isBlank()) {
				throw new ApplicationException("Confirmação por pagamento e motivo são obrigatórios.");
			}
			if (byPayment.putIfAbsent(confirmation.paymentId(), confirmation) != null) {
				throw new ApplicationException("Confirmação duplicada.");
			}
		}
		return new CancellationConfirmations(byPayment);
	}

}
