package com.klaus.moply.shared.application.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.klaus.moply.shared.domain.exception.DomainException;

class PageQueryTest {

	@Test
	void shouldProvideStableDefaultBoundsAndParseDirectionWithoutFrameworkTypes() {
		assertEquals(new PageQuery(0, 20, null), PageQuery.defaults());
		assertEquals(100, PageQuery.MAX_SIZE);
		assertEquals(SortQuery.Direction.DESC, SortQuery.Direction.parse("desc"));
	}

	@Test
	void shouldRejectInvalidPageSizeAndSortDirection() {
		assertThrows(DomainException.class, () -> new PageQuery(-1, 20, null));
		assertThrows(DomainException.class, () -> new PageQuery(0, 0, null));
		assertThrows(DomainException.class, () -> new PageQuery(0, 101, null));
		assertThrows(DomainException.class, () -> SortQuery.Direction.parse("sideways"));
	}

}
