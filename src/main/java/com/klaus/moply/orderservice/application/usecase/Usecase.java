package com.klaus.moply.orderservice.application.usecase;

@FunctionalInterface
public interface Usecase<Input, Output> {

	Output execute(Input input);

}
