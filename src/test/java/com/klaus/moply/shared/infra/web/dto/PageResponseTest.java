package com.klaus.moply.shared.infra.web.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.IntStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.klaus.moply.shared.application.pagination.PageResult;

class PageResponseTest {

	@ParameterizedTest
	@ValueSource(ints = { 20, 1, 0 })
	void shouldPreserveRequestedSizeAndMetadataForFullPartialAndEmptyPages(int returnedCount) {
		var content = IntStream.range(0, returnedCount).boxed().toList();
		var result = new PageResult<>(content, 1, 20, 21, 2);
		var response = PageResponse.from(result, Object::toString);

		assertEquals(20, result.size());
		assertEquals(content.stream().map(Object::toString).toList(), response.content());
		assertEquals(1, response.page());
		assertEquals(20, response.size());
		assertEquals(21, response.totalElements());
		assertEquals(2, response.totalPages());
		assertEquals(response, PageResponse.from(result.map(Object::toString), value -> value));
	}

}
