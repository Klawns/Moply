package com.klaus.moply.shared.domain.vo;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.klaus.moply.shared.domain.exception.DomainException;

class MoneyTest {

	@ParameterizedTest
	@CsvSource({ ",Valor monetário é obrigatório.", "-1,Valor monetário não pode ser negativo.",
			"1.230,Valor monetário deve ter no máximo duas casas decimais." })
	void shouldExplainInvalidMoney(String input, String message) {
		BigDecimal value = input == null ? null : new BigDecimal(input);
		assertEquals(message, assertThrows(DomainException.class, () -> new Money(value)).getMessage());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "-1", "-0.001", "1.230", "0.001" })
	void shouldRejectInvalidMoney(String input) {
		assertThrows(DomainException.class, () -> new Money(input == null ? null : new BigDecimal(input)));
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "0.00", "0.01", "1", "1.2", "1E+3", "92233720368547758.09" })
	void shouldRepresentExactNonNegativeGbp(String input) {
		var money = new Money(new BigDecimal(input));
		assertEquals(new BigDecimal(input).setScale(2), money.value());
		assertEquals(Currency.getInstance("GBP"), money.currency());
		assertEquals(money, new Money(money.value()));
	}

}
