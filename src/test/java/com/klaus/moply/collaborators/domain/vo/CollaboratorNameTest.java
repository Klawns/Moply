package com.klaus.moply.collaborators.domain.vo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.klaus.moply.shared.domain.exception.DomainException;

import static org.junit.jupiter.api.Assertions.*;

class CollaboratorNameTest {

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t\n", "\u2003" })
	void shouldRejectMissingOrBlankNames(String value) {
		assertThrows(DomainException.class, () -> new CollaboratorName(value));
	}

	@Test
	void shouldStripWhitespaceAndCompareNamesByValue() {
		var name = new CollaboratorName("\u2003 Maria da Silva \t");
		assertEquals("Maria da Silva", name.value());
		assertEquals(new CollaboratorName("Maria da Silva"), name);
		assertEquals(new CollaboratorName("Maria da Silva").hashCode(), name.hashCode());
	}

}
