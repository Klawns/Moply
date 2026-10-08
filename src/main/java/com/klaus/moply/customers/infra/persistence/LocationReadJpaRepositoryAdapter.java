package com.klaus.moply.customers.infra.persistence;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import com.klaus.moply.customers.application.ports.LocationReadRepository;
import com.klaus.moply.customers.application.usecase.dto.LocationSummary;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.infra.persistence.PageableMapper;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationReadJpaRepositoryAdapter implements LocationReadRepository {

	private final CustomerLocationJpaRepository repository;

	@Override
	public PageResult<LocationSummary> findAll(UUID organizationId, String query, PageQuery page) {
		var account = java.util.Objects.requireNonNull(organizationId);
		Specification<CustomerLocationEntity> specification = (root, criteria, builder) -> {
			var scope = builder.equal(root.get("organizationId"), account);
			if (query == null || query.isBlank())
				return scope;
			var pattern = "%"
					+ query.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
					+ "%";
			return builder.and(scope,
					builder.or(builder.like(builder.lower(root.get("name")), pattern, '\\'),
							builder.like(builder.lower(root.get("address")), pattern, '\\'),
							builder.like(builder.lower(root.get("customer").get("name")), pattern, '\\')));
		};
		var result = repository.findAll(specification, PageableMapper.toPageable(page, Set.of("name", "id"), "name"));
		return PageableMapper.toResult(result, location -> new LocationSummary(location.getId(), location.getName(),
				location.getAddress(), location.getCustomerId(), location.getCustomer().getName()));
	}

}
