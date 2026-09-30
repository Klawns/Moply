package com.klaus.moply.collaborators.infra.persistence;

import java.util.UUID;

import com.klaus.moply.collaborators.domain.entities.Collaborator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_collaborator", uniqueConstraints = @UniqueConstraint(columnNames = { "organization_id", "id" }))
@Getter
@NoArgsConstructor
public class CollaboratorEntity {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;

	@Column(nullable = false, columnDefinition = "text")
	private String name;

	@Column(columnDefinition = "text")
	private String phone;

	@Column(nullable = false)
	private boolean active;

	@Version
	private long version;

	public CollaboratorEntity(UUID organizationId) {
		this.id = UUID.randomUUID();
		this.organizationId = organizationId;
	}

	public void update(Collaborator collaborator) {
		name = collaborator.getName().value();
		phone = collaborator.getPhone() == null ? null : collaborator.getPhone().value();
		active = collaborator.isActive();
	}

	public Collaborator toDomain() {
		return Collaborator.restore(id, organizationId, name, phone, active, version);
	}

}
