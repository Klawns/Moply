package com.klaus.moply.auth.infra.persistence.entities;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_revoked_token")
@Getter
@NoArgsConstructor
public class RevokedTokenEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private Instant expiresAt;

	public RevokedTokenEntity(UUID id, Instant expiresAt) {
		this.id = id;
		this.expiresAt = expiresAt;
	}

}
