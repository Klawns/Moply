package com.klaus.moply.shared.domain.vo;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.klaus.moply.shared.domain.exception.DomainException;

class PhoneTest {

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t\n", "\u2003" })
	void shouldRepresentAbsentPhoneWithoutAnInvalidValueObject(String value) {
		assertNull(Phone.ofNullable(value));
		assertThrows(DomainException.class, () -> new Phone(value));
	}

	@ParameterizedTest
	@ValueSource(strings = { "+55 (11) 99999-1234", "telefone livre", "123", "Ramal  42" })
	void shouldPreserveFreeTextAndNormalizeEdges(String value) {
		var phone = Phone.ofNullable("\u2003 " + value + " \t");
		assertEquals(value, phone.value());
		assertEquals(new Phone(value), phone);
		assertEquals(new Phone(value).hashCode(), phone.hashCode());
	}

}
