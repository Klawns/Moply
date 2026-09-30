package com.klaus.moply.accounts.infra.persistence.entities;

import java.util.UUID;

import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.vo.LoginEmail;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_app_user", uniqueConstraints = @UniqueConstraint(name = "uk_app_user_email", columnNames = "email"))
@Getter
@NoArgsConstructor
public class AppUserEntity {

	@Id
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organization_id", nullable = false, unique = true)
	private OrganizationEntity organization;

	@Column(nullable = false, columnDefinition = "text")
	private String email;

	@Column(name = "password_hash", nullable = false, columnDefinition = "text")
	private String passwordHash;

	public AppUserEntity(AppUser user, OrganizationEntity organization) {
		id = user.getId();
		this.organization = organization;
		email = user.getEmail().value();
		passwordHash = user.getPasswordHash();
	}

	public AppUser toDomain() {
		return new AppUser(id, organization.getId(), new LoginEmail(email), passwordHash);
	}

}
