package com.klaus.moply.recurrence.infra.persistence;

import java.util.*;
import com.klaus.moply.recurrence.application.ports.RecurrenceChanges;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import lombok.RequiredArgsConstructor;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.pagination.SortQuery;
import com.klaus.moply.shared.domain.exception.DomainException;
import jakarta.persistence.EntityManager;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecurrenceChangesJpaAdapter implements RecurrenceChanges {

	private final RecurrenceCommandJpaRepository commands;

	private final RecurrenceChangeItemJpaRepository items;

	private final RecurrenceExclusionJpaRepository exclusions;

	private final EntityManager entityManager;

	public Optional<Command> find(UUID account, UUID family, String key) {
		return commands.findByOrganizationIdAndFamilyIdAndCommandKey(account, family, key)
			.map(e -> new Command(e.getId(), account, family, e.getCommandKey(), e.getContent(), e.getActorId(),
					e.getRecordedAt(), e.getSuccessorId()));
	}

	public Optional<Command> command(UUID account, UUID id) {
		return commands.findByOrganizationIdAndId(account, id)
			.map(e -> new Command(e.getId(), account, e.getFamilyId(), e.getCommandKey(), e.getContent(),
					e.getActorId(), e.getRecordedAt(), e.getSuccessorId()));
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void save(Command c) {
		var e = new RecurrenceCommandEntity();
		e.setId(c.id());
		e.setOrganizationId(c.account());
		e.setFamilyId(c.family());
		e.setCommandKey(c.key());
		e.setContent(c.content());
		e.setActorId(c.actor());
		e.setRecordedAt(c.at());
		e.setSuccessorId(c.successor());
		commands.saveAndFlush(e);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void record(Item i) {
		var e = new RecurrenceChangeItemEntity();
		e.setId(i.id());
		e.setOrganizationId(i.account());
		e.setCommandId(i.command());
		e.setWorkId(i.work());
		e.setPosition(i.position());
		e.setReason(i.reason());
		e.setTargetSeriesId(i.targetSeries());
		e.setServiceDateBefore(i.serviceDateBefore());
		e.setServiceDateAfter(i.serviceDateAfter());
		items.saveAndFlush(e);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void exclude(UUID account, UUID family, long position, UUID work) {
		if (excluded(account, family, position))
			return;
		var e = new RecurrenceExclusionEntity();
		e.setId(UUID.randomUUID());
		e.setOrganizationId(account);
		e.setFamilyId(family);
		e.setPosition(position);
		e.setWorkId(work);
		exclusions.saveAndFlush(e);
	}

	public boolean excluded(UUID account, UUID family, long position) {
		return exclusions.existsByOrganizationIdAndFamilyIdAndPosition(account, family, position);
	}

	public PageResult<Item> history(UUID account, UUID work, PageQuery page) {
		var sort = page.sort();
		String field = sort == null ? "at" : sort.field();
		if (!Set.of("at", "id").contains(field))
			throw new DomainException("Campo de ordenação não permitido.");
		long offset = (long) page.page() * page.size();
		if (offset > Integer.MAX_VALUE)
			throw new DomainException("Offset do histórico deve ser menor ou igual a 2147483647.");
		String direction = sort == null || sort.direction() == SortQuery.Direction.ASC ? "asc" : "desc";
		String order = field.equals("at") ? "c.recordedAt " + direction + ", c.id asc, i.id asc" : "i.id " + direction;
		var rows = entityManager
			.createQuery("select i from RecurrenceChangeItemEntity i, RecurrenceCommandEntity c "
					+ "where i.organizationId=:account and i.workId=:work and c.organizationId=i.organizationId "
					+ "and c.id=i.commandId order by " + order, RecurrenceChangeItemEntity.class)
			.setParameter("account", account)
			.setParameter("work", work)
			.setFirstResult((int) offset)
			.setMaxResults(page.size())
			.getResultList()
			.stream()
			.map(e -> new Item(e.getId(), account, e.getCommandId(), work, e.getPosition(), e.getReason(),
					e.getTargetSeriesId(), e.getServiceDateBefore(), e.getServiceDateAfter()))
			.toList();
		long total = entityManager
			.createQuery("select count(i) from RecurrenceChangeItemEntity i "
					+ "where i.organizationId=:account and i.workId=:work", Long.class)
			.setParameter("account", account)
			.setParameter("work", work)
			.getSingleResult();
		int pages = total == 0 ? 0 : (int) ((total + page.size() - 1) / page.size());
		return new PageResult<>(rows, page.page(), page.size(), total, pages);
	}

}
