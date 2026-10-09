package com.klaus.moply.workorders.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import com.klaus.moply.accounts.application.usecase.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.collaborators.application.usecase.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException;
import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.exception.ApplicationException;
import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;
import com.klaus.moply.workorders.application.usecase.dto.PricingPreviewOutput;
import com.klaus.moply.workorders.application.usecase.exception.PricingAcceptanceException;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.policy.FixedRateAllocationPolicy;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;
import com.klaus.moply.workorders.domain.vo.WorkOrderAssignments;
import com.klaus.moply.workorders.domain.vo.WorkOrderDescription;
import com.klaus.moply.workorders.domain.vo.WorkOrderPricing;
import com.klaus.moply.workorders.domain.vo.WorkOrderSchedule;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorkOrderPreparation {

	private final CustomerRepository customers;

	private final CollaboratorRepository collaborators;

	private final OrganizationRepository accounts;

	public PricingPreviewOutput preview(Usecase.Context context, CreateWorkOrderInput input) {
		return calculate(context, input).preview();
	}

	/** Validates the creation contract and acceptance without persisting. */
	public WorkOrder prepare(Usecase.Context context, CreateWorkOrderInput input) {
		var calculated = calculate(context, input);
		var preview = calculated.preview();
		if (!preview.canCreate()
				|| (preview.requiresConfirmation()
						&& !preview.pricingFingerprint().equals(input.acceptedPricingFingerprint()))
				|| (input.acceptedPricingFingerprint() != null
						&& !preview.pricingFingerprint().equals(input.acceptedPricingFingerprint())))
			throw new PricingAcceptanceException(preview);
		return WorkOrder.createPriced(input.customerId(), input.customerLocationId(),
				new WorkOrderSchedule(input.serviceDate(), input.startTime()),
				new WorkOrderDescription(input.description()),
				new WorkOrderPricing(new DurationHours(preview.contractedHours()), new HourlyRate(preview.hourlyRate()),
						preview.currencyCode(), calculated.result().total(), preview.allocationPolicyVersion()),
				new WorkOrderAssignments(calculated.result().assignments()), calculated.status());
	}

	/** Recurrences use approved frozen conditions, never the current tariffs. */
	public WorkOrder prepareFrozen(Usecase.Context context, CreateWorkOrderInput input, WorkOrderPricing pricing,
			WorkOrderAssignments assignments) {
		validateReferences(context, input);
		var work = pricing == null
				? WorkOrder.create(input.customerId(), input.customerLocationId(),
						new WorkOrderSchedule(input.serviceDate(), input.startTime()),
						new WorkOrderDescription(input.description()), new DurationHours(input.contractedHours()),
						new HourlyRate(input.hourlyRate()), input.participantIds(), input.initialStatus())
				: WorkOrder.createPriced(input.customerId(), input.customerLocationId(),
						new WorkOrderSchedule(input.serviceDate(), input.startTime()),
						new WorkOrderDescription(input.description()), pricing, assignments, input.initialStatus());
		return work;
	}

	private Calculated calculate(Usecase.Context context, CreateWorkOrderInput input) {
		var participants = validateReferences(context, input);
		var organization = accounts.findById(context.organizationId()).orElseThrow(AccountNotFoundException::new);
		var hours = new DurationHours(input.contractedHours());
		var rate = new HourlyRate(input.hourlyRate() == null ? organization.defaultHourlyRate() : input.hourlyRate());
		var rates = new ArrayList<BigDecimal>();
		for (var person : participants) {
			rates.add(person.getHourlyRate() == null ? null : person.getHourlyRate().value());
		}
		var result = new FixedRateAllocationPolicy().calculate(hours, rate, input.participantIds(), rates);
		var status = input.initialStatus() == null ? WorkOrderStatus.valueOf(organization.defaultWorkStatus().name())
				: input.initialStatus();
		if (status == WorkOrderStatus.CANCELLED)
			throw new ApplicationException("Estado inicial inválido.");
		var individualHours = hours.value()
			.divide(BigDecimal.valueOf(input.participantIds().size()), 8, RoundingMode.HALF_EVEN);
		var rows = result.assignments()
			.stream()
			.map(a -> new PricingPreviewOutput.Participant(a.collaboratorId(), a.inclusionPosition(),
					a.appliedHourlyRate().value(), a.fixedRate() ? "COLLABORATOR" : "WORK_ORDER", individualHours,
					a.baseAmount().value(), a.surplusAmount().value(), a.allocatedAmount().value()))
			.toList();
		var fingerprint = fingerprint(context, input, hours, rate, status, result);
		var preview = new PricingPreviewOutput(fingerprint, "GBP", hours.value(), rate.value(), result.total().value(),
				result.policyVersion(), result.requiresConfirmation(), result.canCreate(), result.baseTotal(),
				result.surplusAmount(), result.excessAmount(), rows.size(), rows);
		return new Calculated(preview, result, status);
	}

	private List<Collaborator> validateReferences(Usecase.Context context, CreateWorkOrderInput input) {
		if (input == null || input.customerId() == null)
			throw new ApplicationException("Cliente obrigatório.");
		new WorkOrderSchedule(input.serviceDate(), input.startTime());
		new WorkOrderDescription(input.description());
		var customer = customers.findById(context.organizationId(), input.customerId())
			.orElseThrow(() -> new CustomerNotFoundException(input.customerId()));
		if (input.customerLocationId() != null)
			customer.findLocation(input.customerLocationId());
		if (input.participantIds() == null || input.participantIds().isEmpty()
				|| input.participantIds().stream().anyMatch(Objects::isNull)
				|| new HashSet<>(input.participantIds()).size() != input.participantIds().size())
			throw new ApplicationException("Participantes devem ter IDs não nulos e únicos.");
		var participants = new ArrayList<Collaborator>();
		for (var id : input.participantIds()) {
			var person = collaborators.findById(context.organizationId(), id)
				.orElseThrow(() -> new CollaboratorNotFoundException(id));
			if (!person.isActive())
				throw new InactiveCollaboratorException();
			participants.add(person);
		}
		return participants;
	}

	private String fingerprint(Usecase.Context context, CreateWorkOrderInput input, DurationHours hours,
			HourlyRate rate, WorkOrderStatus status, FixedRateAllocationPolicy.Result result) {
		// Length-prefixed fields avoid ambiguity; the acceptance itself is excluded.
		var fields = Arrays.asList(context.organizationId(), input.customerId(), input.customerLocationId(),
				input.serviceDate(), input.startTime(), input.description(), hours.value(), rate.value(), status,
				result.policyVersion(), result.total().value(), result.assignments());
		var canonical = new StringBuilder();
		for (var field : fields) {
			String value = field == null ? "" : field.toString();
			canonical.append(field == null ? -1 : value.length()).append(':').append(value);
		}
		try {
			return HexFormat.of()
				.formatHex(MessageDigest.getInstance("SHA-256")
					.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private record Calculated(PricingPreviewOutput preview, FixedRateAllocationPolicy.Result result,
			WorkOrderStatus status) {
	}

}
