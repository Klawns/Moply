package com.klaus.moply.factory;

import java.util.UUID;
import com.klaus.moply.collaborators.domain.entities.Collaborator;

public final class CollaboratorFactory {

	private CollaboratorFactory() {
	}

	public static Collaborator create(UUID organizationId) {
		return Collaborator.create(organizationId, "Maria", null);
	}

	public static Collaborator persisted(UUID organizationId) {
		return Collaborator.restore(UUID.randomUUID(), organizationId, "Maria", null, true, 0);
	}

}
