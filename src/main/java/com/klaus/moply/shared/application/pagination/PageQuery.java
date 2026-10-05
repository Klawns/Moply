package com.klaus.moply.shared.application.pagination;

import com.klaus.moply.shared.domain.exception.DomainException;

/** Zero-based application pagination. Invalid bounds fail before reaching persistence. */
public record PageQuery(int page, int size, SortQuery sort) {

	public static final int DEFAULT_PAGE = 0;

	public static final int DEFAULT_SIZE = 20;

	public static final int MAX_SIZE = 100;

	public PageQuery {
		if (page < 0)
			throw new DomainException("Página deve ser maior ou igual a zero.");
		if (size < 1 || size > MAX_SIZE)
			throw new DomainException("Tamanho deve estar entre 1 e " + MAX_SIZE + ".");
	}

	public static PageQuery defaults() {
		return new PageQuery(DEFAULT_PAGE, DEFAULT_SIZE, null);
	}
}
