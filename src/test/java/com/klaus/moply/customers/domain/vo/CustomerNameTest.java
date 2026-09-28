package com.klaus.moply.customers.domain.vo;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import com.klaus.moply.orderservice.domain.exception.DomainException;

class CustomerNameTest {

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t\n", "\u2003" })
	void shouldRejectAbsentName(String value) {
		assertThrows(DomainException.class, () -> new CustomerName(value));
	}

	@Test
	void shouldNormalizeEdgesAndPreserveCaseAndInternalSpaces() {
		var name = new CustomerName("\u2003 João  da Silva \t");
		assertEquals("João  da Silva", name.value());
		assertEquals(new CustomerName("João  da Silva"), name);
		assertEquals(new CustomerName("João  da Silva").hashCode(), name.hashCode());
	}

}
