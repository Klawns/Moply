package com.klaus.moply.workorders.domain.vo;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.klaus.moply.shared.domain.exception.DomainException;

class CalculationInputsTest {

	@ParameterizedTest
	@CsvSource({ ",Horas são obrigatórias.,Tarifa por hora é obrigatória.",
			"0,Horas devem ser maiores que zero.,Tarifa por hora deve ser maior que zero.",
			"-1,Horas devem ser maiores que zero.,Tarifa por hora deve ser maior que zero.",
			"1.230,Horas devem ter no máximo duas casas decimais.,Tarifa por hora deve ter no máximo duas casas decimais." })
	void shouldExplainInvalidInputs(String input, String hoursMessage, String rateMessage) {
		BigDecimal value = input == null ? null : new BigDecimal(input);
		assertEquals(hoursMessage, assertThrows(DomainException.class, () -> new DurationHours(value)).getMessage());
		assertEquals(rateMessage, assertThrows(DomainException.class, () -> new HourlyRate(value)).getMessage());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "0", "0.00", "-1", "-0.01", "1.230", "0.001" })
	void shouldRejectInvalidInputs(String input) {
		BigDecimal value = input == null ? null : new BigDecimal(input);
		assertThrows(DomainException.class, () -> new DurationHours(value));
		assertThrows(DomainException.class, () -> new HourlyRate(value));
	}

	@ParameterizedTest
	@ValueSource(strings = { "1", "1.2", "1.23", "0.01", "0.5", "1E+3" })
	void shouldNormalizeValidInputsWithoutRounding(String input) {
		var value = new BigDecimal(input);
		assertEquals(value.setScale(2), new DurationHours(value).value());
		assertEquals(value.setScale(2), new HourlyRate(value).value());
	}

}
