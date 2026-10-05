package com.klaus.moply.shared.infra.web;

import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.SortQuery;

/** HTTP-only parsing for the shared page/pageSize/sort/direction query parameters. */
public final class PageQueryRequest {

	private PageQueryRequest() {
	}

	public static PageQuery toQuery(Integer page, Integer size, String sort, String direction) {
		int resolvedPage = page == null ? PageQuery.DEFAULT_PAGE : page;
		int resolvedSize = size == null ? PageQuery.DEFAULT_SIZE : size;
		SortQuery order = sort == null && direction == null ? null : new SortQuery(sort == null ? "" : sort,
				direction == null ? SortQuery.Direction.ASC : SortQuery.Direction.parse(direction));
		return new PageQuery(resolvedPage, resolvedSize, order);
	}

}
