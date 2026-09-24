package com.klaus.moply.application.usecase;

import java.util.UUID;

import com.klaus.moply.application.ports.OrderServiceRepository;
import com.klaus.moply.application.usecase.exception.OrderServiceNotFoundException;
import com.klaus.moply.domain.entity.OrderService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteOrderService implements Usecase<UUID, Void> {

    private final OrderServiceRepository repo;

    @Override
    public Void execute(UUID id) {

        OrderService orderService = repo.findById(id)
                .orElseThrow(() -> new OrderServiceNotFoundException(id));

        repo.deleteById(orderService.getId());

        return null;
    }
}
