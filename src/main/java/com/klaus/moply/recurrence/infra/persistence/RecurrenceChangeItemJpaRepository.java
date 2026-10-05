package com.klaus.moply.recurrence.infra.persistence;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurrenceChangeItemJpaRepository extends JpaRepository<RecurrenceChangeItemEntity, UUID> {

}
