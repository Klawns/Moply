package com.klaus.moply.accounts.application.ports;

import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.vo.Organization;

public interface AccountRegistration {

	void register(Organization organization, AppUser manager);

}
