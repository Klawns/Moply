package com.klaus.moply.auth.infra.persistence.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_auth_rate_guard")
public class AuthRateGuardEntity {

	@Id
	private Integer id;

	protected AuthRateGuardEntity() {
	}

}
