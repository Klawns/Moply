package com.klaus.moply.collaborators.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.klaus.moply.collaborators.domain.vo.CollaboratorName;
import com.klaus.moply.shared.domain.vo.Phone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.application.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.application.usecase.*;
import com.klaus.moply.collaborators.application.usecase.dto.*;
import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException;
import com.klaus.moply.factory.CollaboratorFactory;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;

class CollaboratorUsecasesTest {

	private final CollaboratorRepository repo = mock(CollaboratorRepository.class);

	private final UUID account = UUID.randomUUID();

	@Test
	void shouldCreateInTrustedAccountAndReturnSavedIdentity() {
		var saved = CollaboratorFactory.persisted(account);
		when(repo.save(eq(account), any(Collaborator.class))).thenReturn(saved);
		assertEquals(saved.getId(), new CreateCollaborator(repo).execute(new Context(account),
				new CreateCollaboratorInput(" Maria ", null)));
		var captured = ArgumentCaptor.forClass(Collaborator.class);
		verify(repo).save(eq(account), captured.capture());
		assertEquals(account, captured.getValue().getOrganizationId());
		assertEquals(new CollaboratorName("Maria"), captured.getValue().getName());
		assertNull(captured.getValue().getPhone());
		assertNull(captured.getValue().getId());
		assertTrue(captured.getValue().isActive());
	}

	@Test
	void shouldFindAndFilterWithinAccountIncludingDedicatedEligibilityQuery() {
		var saved = CollaboratorFactory.persisted(account);
		when(repo.findById(account, saved.getId())).thenReturn(Optional.of(saved));
		when(repo.findAll(account, null, PageQuery.defaults()))
			.thenReturn(new PageResult<>(List.of(saved, saved.deactivate()), 0, 20, 2, 1));
		when(repo.findAll(account, false, PageQuery.defaults()))
			.thenReturn(new PageResult<>(List.of(saved.deactivate()), 0, 20, 1, 1));
		when(repo.findAllActive(account)).thenReturn(List.of(saved));
		assertEquals(saved.getId(), new FindCollaboratorById(repo).execute(new Context(account), saved.getId()).id());
		assertEquals(2,
				new FindAllCollaborators(repo)
					.execute(new Context(account), new FindAllCollaborators.Filter(null, PageQuery.defaults()))
					.totalElements());
		assertFalse(new FindAllCollaborators(repo)
			.execute(new Context(account), new FindAllCollaborators.Filter(false, PageQuery.defaults()))
			.content()
			.getFirst()
			.active());
		assertEquals(List.of(CollaboratorOutput.fromDomain(saved)),
				new FindEligibleCollaborators(repo).execute(new Context(account), null));
		verify(repo).findAllActive(account);
	}

	@Test
	void shouldUpdateWithoutChangingAccountIdentityOrState() {
		var saved = CollaboratorFactory.persisted(account);
		when(repo.findById(account, saved.getId())).thenReturn(Optional.of(saved));
		when(repo.save(eq(account), any(Collaborator.class))).thenAnswer(invocation -> invocation.getArgument(1));
		var output = new UpdateCollaborator(repo).execute(new Context(account),
				new UpdateCollaboratorInput(saved.getId(), "Ana", "123"));
		assertEquals(saved.getId(), output.id());
		assertEquals("Ana", output.name());
		assertTrue(output.active());
		var captured = ArgumentCaptor.forClass(Collaborator.class);
		verify(repo).save(eq(account), captured.capture());
		assertEquals(saved.getId(), captured.getValue().getId());
		assertEquals(account, captured.getValue().getOrganizationId());
		assertEquals(new CollaboratorName("Ana"), captured.getValue().getName());
		assertEquals(new Phone("123"), captured.getValue().getPhone());
		assertEquals(saved.getVersion(), captured.getValue().getVersion());
	}

	@Test
	void shouldDeactivateOnceAndBlockInactiveUpdates() {
		var saved = CollaboratorFactory.persisted(account);
		when(repo.findById(account, saved.getId())).thenReturn(Optional.of(saved))
			.thenReturn(Optional.of(saved.deactivate()));
		var deactivate = new DeactivateCollaborator(repo);
		deactivate.execute(new Context(account), saved.getId());
		deactivate.execute(new Context(account), saved.getId());
		var captured = ArgumentCaptor.forClass(Collaborator.class);
		verify(repo).save(eq(account), captured.capture());
		assertEquals(saved.getId(), captured.getValue().getId());
		assertEquals(account, captured.getValue().getOrganizationId());
		assertFalse(captured.getValue().isActive());
		assertThrows(InactiveCollaboratorException.class, () -> new UpdateCollaborator(repo)
			.execute(new Context(account), new UpdateCollaboratorInput(saved.getId(), "Ana", null)));
		verify(repo, times(1)).save(any(), any());
	}

	@Test
	void shouldRejectMissingOrForeignResourcesWithoutWriting() {
		UUID id = UUID.randomUUID();
		when(repo.findById(account, id)).thenReturn(Optional.empty());
		assertThrows(CollaboratorNotFoundException.class,
				() -> new FindCollaboratorById(repo).execute(new Context(account), id));
		assertThrows(CollaboratorNotFoundException.class, () -> new UpdateCollaborator(repo)
			.execute(new Context(account), new UpdateCollaboratorInput(id, "Ana", null)));
		assertThrows(CollaboratorNotFoundException.class,
				() -> new DeactivateCollaborator(repo).execute(new Context(account), id));
		verify(repo, never()).save(any(), any());
	}

}
