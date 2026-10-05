package com.klaus.moply.shared.infra.persistence;

import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.pagination.SortQuery;
import com.klaus.moply.shared.domain.exception.DomainException;

/** Translation boundary between application-owned paging contracts and Spring Data. */
public final class PageableMapper {

	private PageableMapper() {
	}

	public static Pageable toPageable(PageQuery query, Set<String> allowedFields, String defaultField) {
		var requested = query.sort();
		var field = requested == null ? defaultField : requested.field();
		if (!allowedFields.contains(field))
			throw new DomainException("Campo de ordenação não permitido.");
		var direction = requested == null || requested.direction() == SortQuery.Direction.ASC ? Sort.Direction.ASC
				: Sort.Direction.DESC;
		var sort = Sort.by(direction, field);
		if (!"id".equals(field))
			sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
		return PageRequest.of(query.page(), query.size(), sort);
	}

	public static <S, T> PageResult<T> toResult(org.springframework.data.domain.Page<S> page,
			java.util.function.Function<S, T> mapper) {
		return new PageResult<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}

}
