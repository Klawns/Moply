package com.klaus.moply.workorders.infra.persistence;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.workorders.domain.vo.WorkOrderDateRange;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkOrderPaginationTest {

	@Test
	void shouldSkipAssignmentsQueryForEmptyAndOutOfRangePages() {
		for (long total : new long[] { 0, 3 }) {
			var repository = mock(WorkOrderJpaRepository.class);
			var adapter = new WorkOrderJpaRepositoryAdapter(repository, mock(CustomerRepository.class),
					mock(CollaboratorRepository.class));
			when(repository.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<WorkOrderEntity>(List.of(), PageRequest.of(4, 2), total));
			var result = adapter.search(UUID.randomUUID(), new WorkOrderDateRange(null, null), null, null,
					new PageQuery(4, 2, null));
			assertTrue(result.isEmpty());
			assertEquals(4, result.page());
			assertEquals(total, result.totalElements());
			assertEquals(total == 0 ? 0 : 2, result.totalPages());
			verify(repository, never()).findAllByOrganizationIdAndIdIn(any(), any());
		}
	}

}
