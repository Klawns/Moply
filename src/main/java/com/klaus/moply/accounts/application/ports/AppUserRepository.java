package com.klaus.moply.accounts.application.ports;

import java.util.Optional;

import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.vo.LoginEmail;

public interface AppUserRepository {

	Optional<AppUser> findById(java.util.UUID id);

	Optional<AppUser> findByEmail(LoginEmail email);

}
