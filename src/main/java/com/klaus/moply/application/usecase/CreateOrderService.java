package com.klaus.moply.application.usecase;

import com.klaus.moply.application.dto.CreateOrderServiceInput;
import com.klaus.moply.application.dto.OrderServiceOutput;
import com.klaus.moply.application.ports.OrderServiceRepository;
import com.klaus.moply.domain.entity.OrderService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateOrderService implements Usecase<CreateOrderServiceInput, OrderServiceOutput> {

    private final OrderServiceRepository repo;

    public OrderServiceOutput execute(CreateOrderServiceInput input) {
        OrderService orderService = OrderService.create(
                input.customer(),
                input.contractedHours(),
                input.HourlyPrice(),
                input.employeeCount(),
                input.serviceDate());

        orderService = repo.save(orderService);

        return OrderServiceOutput.builder()
                .id(orderService.getId())
                .customer(orderService.getCustomer().name())
                .contractedHours(orderService.getContractedHours().value())
                .totalValue(orderService.CalculateTotalValue().value())
                .individualHour(orderService.calculateIndividualHoursPerEmployee().value())
                .individualPaymentValue(orderService.calculateIndividualPaymentPerEmployee().value())
                .serviceDate(orderService.getServiceDate())
                .employeeCount(orderService.getEmployeeCount())
                .build();
    }
}
