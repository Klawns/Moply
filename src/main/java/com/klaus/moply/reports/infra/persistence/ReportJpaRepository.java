package com.klaus.moply.reports.infra.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.klaus.moply.workorders.infra.persistence.WorkOrderEntity;

public interface ReportJpaRepository extends JpaRepository<WorkOrderEntity, UUID> {

	@Query(value = """
			select w.id as id, w.customer_id as customerId, c.name as customerName,
			w.service_date as serviceDate, w.status as status, w.currency_code as currencyCode,
			w.total_amount as totalAmount,
			exists (
			    select 1 from tb_customer_payment p
			    where p.organization_id = w.organization_id and p.work_order_id = w.id
			    and p.status = 'RECORDED'
			) as hasActivePayment
			from tb_order_service w
			join tb_customer c on c.organization_id = w.organization_id and c.id = w.customer_id
			where w.organization_id = :account and w.service_date between :from and :to
			and w.status <> 'CANCELLED'
			and (cast(:customerId as uuid) is null or w.customer_id = :customerId)
			""", countQuery = """
			select count(*) from tb_order_service w
			where w.organization_id = :account and w.service_date between :from and :to
			and w.status <> 'CANCELLED'
			and (cast(:customerId as uuid) is null or w.customer_id = :customerId)
			""", nativeQuery = true)
	Page<WorkProjection> workRows(UUID account, LocalDate from, LocalDate to, UUID customerId, Pageable pageable);

	@Query(value = """
			select coalesce(sum(case when w.status = 'COMPLETED' and w.service_date <= :today
			    then w.total_amount else 0 end), 0) as realized,
			coalesce(sum(case when w.status = 'COMPLETED' and w.service_date <= :today
			    and not exists (
			        select 1 from tb_customer_payment p
			        where p.organization_id = w.organization_id and p.work_order_id = w.id
			        and p.status = 'RECORDED'
			    ) then w.total_amount else 0 end), 0) as realizedPending,
			coalesce(sum(w.total_amount), 0) as projection
			from tb_order_service w where w.organization_id = :account and w.service_date between :from and :to
			and w.status <> 'CANCELLED'
			and (cast(:customerId as uuid) is null or w.customer_id = :customerId)
			""", nativeQuery = true)
	WorkTotalsProjection workTotals(UUID account, LocalDate from, LocalDate to, UUID customerId, LocalDate today);

	@Query(value = """
			select p.id as id, p.work_order_id as workOrderId, w.customer_id as customerId,
			c.name as customerName, p.paid_on as paidOn, p.currency_code as currencyCode, p.amount as amount
			from tb_customer_payment p
			join tb_order_service w on w.organization_id = p.organization_id and w.id = p.work_order_id
			join tb_customer c on c.organization_id = w.organization_id and c.id = w.customer_id
			where p.organization_id = :account and p.status = 'RECORDED'
			and p.paid_on between :from and :to
			and (cast(:customerId as uuid) is null or w.customer_id = :customerId)
			""", countQuery = """
			select count(*) from tb_customer_payment p
			join tb_order_service w on w.organization_id = p.organization_id and w.id = p.work_order_id
			join tb_customer c on c.organization_id = w.organization_id and c.id = w.customer_id
			where p.organization_id = :account and p.status = 'RECORDED'
			and p.paid_on between :from and :to
			and (cast(:customerId as uuid) is null or w.customer_id = :customerId)
			""", nativeQuery = true)
	Page<PaymentProjection> customerPayments(UUID account, LocalDate from, LocalDate to, UUID customerId,
			Pageable pageable);

	@Query(value = """
			select coalesce(sum(p.amount), 0) from tb_customer_payment p
			join tb_order_service w on w.organization_id = p.organization_id and w.id = p.work_order_id
			where p.organization_id = :account and p.status = 'RECORDED'
			and p.paid_on between :from and :to
			and (cast(:customerId as uuid) is null or w.customer_id = :customerId)
			""", nativeQuery = true)
	BigDecimal customerPaymentTotal(UUID account, LocalDate from, LocalDate to, UUID customerId);

	@Query(value = """
			select a.id as id, w.id as workOrderId, w.customer_id as customerId, c.name as customerName,
			w.service_date as serviceDate, w.status as workStatus, w.currency_code as currencyCode,
			a.collaborator_id as collaboratorId, co.name as collaboratorName,
			a.allocated_amount as allocatedAmount, coalesce(s.amount, 0) as activeSettlements
			from tb_work_assignment a
			join tb_order_service w on w.organization_id = a.organization_id and w.id = a.work_order_id
			join tb_customer c on c.organization_id = w.organization_id and c.id = w.customer_id
			join tb_collaborator co on co.organization_id = a.organization_id and co.id = a.collaborator_id
			left join (
			    select organization_id, work_order_id, collaborator_id, sum(amount) as amount
			    from tb_collaborator_payment
			    where organization_id = :account and status = 'RECORDED'
			    group by organization_id, work_order_id, collaborator_id
			) s on s.organization_id = a.organization_id and s.work_order_id = a.work_order_id
			    and s.collaborator_id = a.collaborator_id
			where w.organization_id = :account and w.service_date between :from and :to
			and w.status <> 'CANCELLED'
			and (cast(:customerId as uuid) is null or w.customer_id = :customerId)
			and (cast(:collaboratorId as uuid) is null or a.collaborator_id = :collaboratorId)
			""", countQuery = """
			select count(*)
			from tb_work_assignment a
			join tb_order_service w on w.organization_id = a.organization_id and w.id = a.work_order_id
			join tb_customer c on c.organization_id = w.organization_id and c.id = w.customer_id
			join tb_collaborator co on co.organization_id = a.organization_id and co.id = a.collaborator_id
			where w.organization_id = :account and w.service_date between :from and :to
			and w.status <> 'CANCELLED'
			and (cast(:customerId as uuid) is null or w.customer_id = :customerId)
			and (cast(:collaboratorId as uuid) is null or a.collaborator_id = :collaboratorId)
			""", nativeQuery = true)
	Page<AssignmentProjection> assignments(UUID account, LocalDate from, LocalDate to, UUID customerId,
			UUID collaboratorId, Pageable pageable);

	@Query(value = """
			with balances as (
			    select a.allocated_amount, w.status, w.service_date, coalesce(s.amount, 0) as settled
			    from tb_work_assignment a
			    join tb_order_service w on w.organization_id = a.organization_id and w.id = a.work_order_id
			    left join (
			        select organization_id, work_order_id, collaborator_id, sum(amount) as amount
			        from tb_collaborator_payment
			        where organization_id = :account and status = 'RECORDED'
			        group by organization_id, work_order_id, collaborator_id
			    ) s on s.organization_id = a.organization_id and s.work_order_id = a.work_order_id
			        and s.collaborator_id = a.collaborator_id
			    where w.organization_id = :account and w.service_date between :from and :to
			    and w.status <> 'CANCELLED'
			    and (cast(:customerId as uuid) is null or w.customer_id = :customerId)
			    and (cast(:collaboratorId as uuid) is null or a.collaborator_id = :collaboratorId)
			)
			select coalesce(sum(allocated_amount), 0) as allocated,
			coalesce(sum(case when status = 'COMPLETED' and service_date <= :today
			    then allocated_amount else 0 end), 0) as realizedAllocated,
			coalesce(sum(case when service_date > :today then allocated_amount else 0 end), 0) as futureAllocated,
			coalesce(sum(allocated_amount - settled), 0) as pending,
			coalesce(sum(case when status = 'COMPLETED' and service_date <= :today
			    then allocated_amount - settled else 0 end), 0) as realizedPending,
			coalesce(sum(case when service_date > :today then allocated_amount - settled else 0 end), 0) as futurePending
			from balances
			""",
			nativeQuery = true)
	CollaboratorTotalsProjection collaboratorTotals(UUID account, LocalDate from, LocalDate to, UUID customerId,
			UUID collaboratorId, LocalDate today);

	@Query(value = """
			select coalesce(sum(p.amount), 0) from tb_collaborator_payment p
			join tb_order_service w on w.organization_id = p.organization_id and w.id = p.work_order_id
			where p.organization_id = :account and p.status = 'RECORDED'
			and p.paid_on between :from and :to
			and (cast(:customerId as uuid) is null or w.customer_id = :customerId)
			and (cast(:collaboratorId as uuid) is null or p.collaborator_id = :collaboratorId)
			""", nativeQuery = true)
	BigDecimal settlementTotal(UUID account, LocalDate from, LocalDate to, UUID customerId, UUID collaboratorId);

	@Query(value = """
			select p.id as id, p.work_order_id as workOrderId, w.customer_id as customerId,
			c.name as customerName, w.service_date as serviceDate, w.status as workStatus,
			p.collaborator_id as collaboratorId, co.name as collaboratorName,
			p.paid_on as paidOn, p.currency_code as currencyCode, p.amount as amount
			from tb_collaborator_payment p
			join tb_order_service w on w.organization_id = p.organization_id and w.id = p.work_order_id
			join tb_customer c on c.organization_id = w.organization_id and c.id = w.customer_id
			join tb_collaborator co on co.organization_id = p.organization_id and co.id = p.collaborator_id
			where p.organization_id = :account and p.status = 'RECORDED'
			and p.paid_on between :from and :to
			and (cast(:customerId as uuid) is null or w.customer_id = :customerId)
			and (cast(:collaboratorId as uuid) is null or p.collaborator_id = :collaboratorId)
			""", countQuery = """
			select count(*) from tb_collaborator_payment p
			join tb_order_service w on w.organization_id = p.organization_id and w.id = p.work_order_id
			join tb_customer c on c.organization_id = w.organization_id and c.id = w.customer_id
			join tb_collaborator co on co.organization_id = p.organization_id and co.id = p.collaborator_id
			where p.organization_id = :account and p.status = 'RECORDED'
			and p.paid_on between :from and :to
			and (cast(:customerId as uuid) is null or w.customer_id = :customerId)
			and (cast(:collaboratorId as uuid) is null or p.collaborator_id = :collaboratorId)
			""", nativeQuery = true)
	Page<SettlementProjection> settlements(UUID account, LocalDate from, LocalDate to, UUID customerId,
			UUID collaboratorId, Pageable pageable);

	interface WorkProjection {

		UUID getId();

		UUID getCustomerId();

		String getCustomerName();

		LocalDate getServiceDate();

		String getStatus();

		String getCurrencyCode();

		BigDecimal getTotalAmount();

		boolean getHasActivePayment();

	}

	interface WorkTotalsProjection {

		BigDecimal getRealized();

		BigDecimal getRealizedPending();

		BigDecimal getProjection();

	}

	interface PaymentProjection {

		UUID getId();

		UUID getWorkOrderId();

		UUID getCustomerId();

		String getCustomerName();

		LocalDate getPaidOn();

		String getCurrencyCode();

		BigDecimal getAmount();

	}

	interface AssignmentProjection {

		UUID getId();

		UUID getWorkOrderId();

		UUID getCustomerId();

		String getCustomerName();

		LocalDate getServiceDate();

		String getWorkStatus();

		String getCurrencyCode();

		UUID getCollaboratorId();

		String getCollaboratorName();

		BigDecimal getAllocatedAmount();

		BigDecimal getActiveSettlements();

	}

	interface CollaboratorTotalsProjection {

		BigDecimal getAllocated();

		BigDecimal getRealizedAllocated();

		BigDecimal getFutureAllocated();

		BigDecimal getPending();

		BigDecimal getRealizedPending();

		BigDecimal getFuturePending();

	}

	interface SettlementProjection {

		UUID getId();

		UUID getWorkOrderId();

		UUID getCustomerId();

		String getCustomerName();

		LocalDate getServiceDate();

		String getWorkStatus();

		UUID getCollaboratorId();

		String getCollaboratorName();

		LocalDate getPaidOn();

		String getCurrencyCode();

		BigDecimal getAmount();

	}

}
