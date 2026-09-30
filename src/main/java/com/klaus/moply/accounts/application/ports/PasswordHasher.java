package com.klaus.moply.accounts.application.ports;

public interface PasswordHasher {

	String encode(String password);

}
