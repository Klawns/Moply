package com.klaus.moply.accounts.infra.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.klaus.moply.accounts.application.ports.AccountRegistration;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.application.ports.PasswordHasher;
import com.klaus.moply.accounts.application.usecase.GetAccountPreferences;
import com.klaus.moply.accounts.application.usecase.RegisterAccount;
import com.klaus.moply.accounts.application.usecase.UpdateAccountPreferences;

@Configuration
public class AccountConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	public RegisterAccount registerAccount(AccountRegistration registration, PasswordHasher passwords) {
		return new RegisterAccount(registration, passwords);
	}

	@Bean
	public GetAccountPreferences getAccountPreferences(OrganizationRepository organizations, Clock clock) {
		return new GetAccountPreferences(organizations, clock);
	}

	@Bean
	public UpdateAccountPreferences updateAccountPreferences(OrganizationRepository organizations) {
		return new UpdateAccountPreferences(organizations);
	}

}
