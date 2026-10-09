package com.klaus.moply.shared.exception;

public abstract class CoreException extends RuntimeException {

	private final ErrorCategory category;

	private final String code;

	protected CoreException(ErrorCategory category, String code, String message) {
		super(message);
		this.category = category;
		this.code = code;
	}

	protected CoreException(ErrorCategory category, String code, String message, Throwable cause) {
		super(message, cause);
		this.category = category;
		this.code = code;
	}

	public ErrorCategory category() {
		return category;
	}

	public String code() {
		return code;
	}

}
