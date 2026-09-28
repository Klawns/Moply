package com.klaus.moply.orderservice.infra.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderServiceJpaRepository extends JpaRepository<OrderServiceEntity, UUID> {

	@Query(value = """
			SELECT orders.*
			FROM tb_order_service orders
			JOIN tb_customer customer ON customer.id = orders.customer_id
			WHERE customer.name = :name
			""", nativeQuery = true)
	List<OrderServiceEntity> findAllByCustomerName(@Param("name") String name);

	List<OrderServiceEntity> findAllByServiceDate(LocalDate serviceDate);

}
