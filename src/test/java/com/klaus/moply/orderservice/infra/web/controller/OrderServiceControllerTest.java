package com.klaus.moply.orderservice.infra.web.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.klaus.moply.customers.domain.vo.CustomerName;
import com.klaus.moply.orderservice.application.usecase.CreateOrderService;
import com.klaus.moply.orderservice.application.usecase.DeleteOrderService;
import com.klaus.moply.orderservice.application.usecase.FindAllOrderServicesByCustomerName;
import com.klaus.moply.orderservice.application.usecase.FindOrderServiceById;
import com.klaus.moply.orderservice.application.usecase.FindOrderServiceByServiceDate;
import com.klaus.moply.orderservice.application.usecase.dto.CreateOrderServiceInput;
import com.klaus.moply.orderservice.application.usecase.dto.OrderServiceOutput;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderServiceController.class)
class OrderServiceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CreateOrderService create;

	@MockitoBean
	private FindOrderServiceByServiceDate findByServiceDate;

	@MockitoBean
	private FindAllOrderServicesByCustomerName findByCustomer;

	@MockitoBean
	private FindOrderServiceById findById;

	@MockitoBean
	private DeleteOrderService delete;

	private static final String BASE_URL = "/api/v1/order-services";

	private static final UUID ORDER_SERVICE_ID = UUID.randomUUID();

	private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 9, 25);

	@Test
	void shouldCreateOrderService() throws Exception {

		when(create.execute(any(CreateOrderServiceInput.class))).thenReturn(ORDER_SERVICE_ID);

		mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(createOrderServiceJson()))
			.andExpect(status().isCreated());

		verify(create).execute(any(CreateOrderServiceInput.class));
	}

	@Test
	void shouldFindOrderServiceById() throws Exception {

		when(findById.execute(ORDER_SERVICE_ID)).thenReturn(orderServiceOutput());

		mockMvc.perform(get(BASE_URL + "/{orderId}", ORDER_SERVICE_ID)).andExpect(status().isOk());

		verify(findById).execute(ORDER_SERVICE_ID);
	}

	@Test
	void shouldDeleteOrderService() throws Exception {

		mockMvc.perform(delete(BASE_URL + "/{orderId}", ORDER_SERVICE_ID)).andExpect(status().isNoContent());

		verify(delete).execute(ORDER_SERVICE_ID);
	}

	@Test
	void shouldFindByServiceDate() throws Exception {

		when(findByServiceDate.execute(SERVICE_DATE)).thenReturn(List.of());

		mockMvc.perform(get(BASE_URL).param("serviceDate", SERVICE_DATE.toString())).andExpect(status().isOk());

		verify(findByServiceDate).execute(SERVICE_DATE);
	}

	@Test
	void shouldFindByCustomer() throws Exception {

		when(findByCustomer.execute(any(CustomerName.class))).thenReturn(List.of());

		mockMvc.perform(get(BASE_URL).param("serviceDate", SERVICE_DATE.toString()).param("customer", "João"))
			.andExpect(status().isOk());

		verify(findByCustomer).execute(any(CustomerName.class));
	}

	@Test
	void shouldReturnBadRequestWhenCustomerIsInvalid() throws Exception {

		mockMvc
			.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
				.content(createOrderServiceJsonWithInvalidCustomer()))
			.andDo(print())
			.andExpect(status().isBadRequest());
	}

	private OrderServiceOutput orderServiceOutput() {
		return OrderServiceOutput.builder()
			.customer("João")
			.contractedHours(new BigDecimal("4.00"))
			.hourlyPrice(new BigDecimal("11.50"))
			.employeeCount(2)
			.serviceDate(SERVICE_DATE)
			.totalAmount(new BigDecimal("46.00"))
			.individualHour(new BigDecimal("2.00"))
			.individualAmount(new BigDecimal("23.00"))
			.build();
	}

	private String createOrderServiceJson() {
		return """
				{
				    "customerId": "11111111-1111-1111-1111-111111111111",
				    "contractedHours": 4.00,
				    "hourlyRate": 11.50,
				    "employeeCount": 2,
				    "serviceDate": "%s"
				}
				""".formatted(SERVICE_DATE);
	}

	private String createOrderServiceJsonWithInvalidCustomer() {
		return """
				{
				    "customerId": null,
				    "contractedHours": 4.00,
				    "hourlyRate": 11.50,
				    "employeeCount": 2,
				    "serviceDate": "%s"
				}
				""".formatted(SERVICE_DATE);
	}

}
