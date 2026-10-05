package com.klaus.moply.recurrence.infra.persistence;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurrenceCommandJpaRepository extends JpaRepository<RecurrenceCommandEntity, UUID> {

	Optional<RecurrenceCommandEntity> findByOrganizationIdAndFamilyIdAndCommandKey(UUID account, UUID family,
			String key);

	Optional<RecurrenceCommandEntity> findByOrganizationIdAndId(UUID account, UUID id);

}
