package com.klaus.moply.application.usecase;

@FunctionalInterface
public interface Usecase<Input, Output> {
    Output execute(Input input);
}
