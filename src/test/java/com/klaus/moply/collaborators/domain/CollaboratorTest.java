package com.klaus.moply.collaborators.domain;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException;
import com.klaus.moply.factory.CollaboratorFactory;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.collaborators.domain.vo.CollaboratorName;
import com.klaus.moply.shared.domain.vo.Phone;

class CollaboratorTest {

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t\n", "\u2003" })
	void shouldRejectBlankNames(String name) {
		var account = UUID.randomUUID();
		assertThrows(DomainException.class, () -> Collaborator.create(account, name, null));
		assertThrows(DomainException.class, () -> CollaboratorFactory.persisted(account).update(name, null));
		assertThrows(DomainException.class,
				() -> Collaborator.restore(UUID.randomUUID(), account, name, null, false, 2));
	}

	@Test
	void shouldNormalizeNameAndOptionalFreeFormPhone() {
		var collaborator = Collaborator.create(UUID.randomUUID(), "  Maria  ", "  +44 (0) 123  ");
		assertEquals(new CollaboratorName("Maria"), collaborator.getName());
		assertEquals(new Phone("+44 (0) 123"), collaborator.getPhone());
		assertTrue(collaborator.isActive());
		assertNull(collaborator.update("Maria", "  ").getPhone());
		assertNull(collaborator.update("Maria", null).getPhone());
		assertThrows(NullPointerException.class, () -> Collaborator.create(null, "Maria", null));
	}

	@Test
	void shouldRestoreIdentityVersionAndValueObjectsWithoutActivating() {
		var id = UUID.randomUUID();
		var account = UUID.randomUUID();
		var restored = Collaborator.restore(id, account, " Maria ", " 123 ", false, 3);
		assertEquals(id, restored.getId());
		assertEquals(account, restored.getOrganizationId());
		assertEquals(3, restored.getVersion());
		assertEquals(new CollaboratorName("Maria"), restored.getName());
		assertEquals(new Phone("123"), restored.getPhone());
		assertFalse(restored.isActive());
		assertThrows(DomainException.class, () -> Collaborator.restore(null, account, "Maria", null, true, 0));
	}

	@Test
	void shouldKeepIdentityWhenUpdatingOrDeactivatingAndBlockInactiveEdits() {
		var original = CollaboratorFactory.persisted(UUID.randomUUID());
		var updated = original.update("Ana", "123");
		assertEquals(original.getId(), updated.getId());
		assertEquals(original.getOrganizationId(), updated.getOrganizationId());
		var inactive = updated.deactivate();
		assertFalse(inactive.isActive());
		assertEquals(updated.getId(), inactive.getId());
		assertEquals(updated.getName(), inactive.getName());
		assertEquals(updated.getPhone(), inactive.getPhone());
		assertSame(inactive, inactive.deactivate());
		assertThrows(InactiveCollaboratorException.class, () -> inactive.update("Changed", null));
	}

}
