package com.klaus.moply.domain.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.klaus.moply.domain.entity.OrderService;
import com.klaus.moply.domain.vo.DurationHours;
import com.klaus.moply.domain.vo.Money;
import com.klaus.moply.factory.OrderServiceFactory;

public class CalculosPrestacaoServicoTest {
    @Test
    @DisplayName("Deve calcular o valor total de uma prestação de serviço.")
    public void shouldCalculateTotalValue() {
        OrderService orderService = OrderServiceFactory.createOrderService();
        Money valorTotal = orderService.CalculateTotalAmount();
        assertEquals(new BigDecimal("46.00"), valorTotal.value());
    }

    @Test
    @DisplayName("Deve calcular as horas individuais por colaborador de uma prestação de serviço.")
    public void shouldCalculateIndividualHoursPerEmployee() {
        OrderService orderService = OrderServiceFactory.createOrderService();
        DurationHours horasIndividuais = orderService.calculateIndividualHoursPerEmployee();
        assertEquals(new BigDecimal("2.00"), horasIndividuais.value());
    }

    @Test
    @DisplayName("Deve calcular o valor individual por colaborador de uma prestação de serviço.")
    public void shouldCalculateIndividualEarningsPerEmployee() {
        OrderService orderService = OrderServiceFactory.createOrderService();
        Money valorIndividual = orderService.calculateIndividualPaymentPerEmployee();
        assertEquals(new BigDecimal("23.00"), valorIndividual.value());
    }

}
