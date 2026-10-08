package com.klaus.moply.customers.application.ports;

import java.util.UUID;
import com.klaus.moply.customers.application.usecase.dto.LocationSummary;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;

public interface LocationReadRepository {

	PageResult<LocationSummary> findAll(UUID organizationId, String query, PageQuery page);

}
