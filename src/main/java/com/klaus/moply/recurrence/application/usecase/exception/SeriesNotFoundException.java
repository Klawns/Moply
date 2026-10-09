package com.klaus.moply.recurrence.application.usecase.exception;

import com.klaus.moply.shared.application.usecase.exception.ApplicationException;

public class SeriesNotFoundException extends ApplicationException {

	public SeriesNotFoundException() {
		super("SERIES_NOT_FOUND", "Série não encontrada.");
	}

}
