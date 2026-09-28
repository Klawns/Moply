package com.klaus.moply.customers.domain.vo;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import com.klaus.moply.orderservice.domain.exception.DomainException;

class EmailTest {

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t\n", "\u2003" })
	void shouldRepresentAbsentEmailWithoutAnInvalidValueObject(String value) {
		assertNull(Email.ofNullable(value));
		assertThrows(DomainException.class, () -> new Email(value));
	}

	@ParameterizedTest
	@ValueSource(strings = { "Maria+work@Example.com", "a@b", "João@Example.com" })
	void shouldNormalizeEdgesAndPreserveBasicAcceptedFormats(String value) {
		var email = Email.ofNullable("\u2003 " + value + " \t");
		assertEquals(value, email.value());
		assertEquals(new Email(value), email);
		assertEquals(new Email(value).hashCode(), email.hashCode());
	}

	@ParameterizedTest
	@ValueSource(strings = { "sem-arroba", "@dominio", "usuario@", "a@@b", "a b@c", "a@b c", "a\tb@c", "a@b\nc",
			"a\u2003b@c", "a\u00a0b@c", "a@b\u00a0" })
	void shouldRejectInvalidEmailsInConstructorAndFactory(String value) {
		assertThrows(DomainException.class, () -> new Email(value));
		assertThrows(DomainException.class, () -> Email.ofNullable(value));
	}

}
