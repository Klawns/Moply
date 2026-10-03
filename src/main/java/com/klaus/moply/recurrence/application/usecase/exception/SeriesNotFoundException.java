package com.klaus.moply.recurrence.application.usecase.exception;

public class SeriesNotFoundException extends RuntimeException {

	public SeriesNotFoundException() {
		super("Série não encontrada.");
	}

}
