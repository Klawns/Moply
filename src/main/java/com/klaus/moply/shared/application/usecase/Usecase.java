package com.klaus.moply.shared.application.usecase;

import java.util.Objects;
import java.util.UUID;

@FunctionalInterface
public interface Usecase<Input, Output> {

	Output execute(Input input);

	@FunctionalInterface
	interface Contextual<Input, Output> {

		Output execute(Context context, Input input);

	}

	record Context(UUID organizationId) {
		public Context {
			Objects.requireNonNull(organizationId, "organizationId");
		}
	}

}
