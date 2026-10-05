package com.klaus.moply.recurrence.infra.persistence;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurrenceExclusionJpaRepository extends JpaRepository<RecurrenceExclusionEntity, UUID> {

	boolean existsByOrganizationIdAndFamilyIdAndPosition(UUID account, UUID family, long position);

}
