package com.klaus.moply.shared.application.pagination;

import java.util.Locale;
import java.util.Objects;

import com.klaus.moply.shared.domain.exception.DomainException;

/** Application-owned ordering request; fields are validated by the module adapter. */
public record SortQuery(String field, Direction direction) {

	public SortQuery {
		Objects.requireNonNull(field, "field");
		Objects.requireNonNull(direction, "direction");
		if (field.isBlank())
			throw new DomainException("Campo de ordenação é obrigatório.");
	}

	public enum Direction {

		ASC, DESC;

		public static Direction parse(String value) {
			try {
				return valueOf(value.toUpperCase(Locale.ROOT));
			}
			catch (RuntimeException exception) {
				throw new DomainException("Direção de ordenação inválida.");
			}
		}

	}
}
