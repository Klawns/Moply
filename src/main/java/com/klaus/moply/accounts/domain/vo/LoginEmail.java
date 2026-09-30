package com.klaus.moply.accounts.domain.vo;

import java.util.Locale;
import com.klaus.moply.shared.domain.exception.DomainException;

public record LoginEmail(String value) {
	public LoginEmail {
		if (value == null) {
			throw new DomainException("E-mail obrigatório.");
		}
		value = value.strip().toLowerCase(Locale.ROOT);
		int at = value.indexOf('@');
		if (at <= 0 || at != value.lastIndexOf('@') || at == value.length() - 1 || value.length() > 254
				|| value.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
			throw new DomainException("E-mail inválido.");
		}
	}
}
