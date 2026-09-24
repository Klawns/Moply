package com.klaus.moply.infra.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderServiceJpaRepository extends JpaRepository<OrderServiceEntity, UUID> {

    List<OrderServiceEntity> findAllByCustomer(String customer);

    List<OrderServiceEntity> findAllByServiceDate(LocalDate serviceDate);
}
