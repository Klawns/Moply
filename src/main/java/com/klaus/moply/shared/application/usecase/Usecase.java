package com.klaus.moply.shared.application.usecase;

@FunctionalInterface
public interface Usecase<Input, Output> {

	Output execute(Input input);

}
