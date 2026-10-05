package com.klaus.moply.collaborators.infra.web.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonSetter;

public class CollaboratorRequest {

	@NotBlank(message = "O nome do colaborador é obrigatório.")
	private String name;

	private String phone;

	private BigDecimal hourlyRate;

	private boolean hourlyRateProvided;

	public CollaboratorRequest() {
	}

	public String name() {
		return name;
	}

	public String phone() {
		return phone;
	}

	public BigDecimal hourlyRate() {
		return hourlyRate;
	}

	public boolean hourlyRateProvided() {
		return hourlyRateProvided;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	@JsonSetter("hourlyRate")
	public void setHourlyRate(BigDecimal hourlyRate) {
		this.hourlyRate = hourlyRate;
		this.hourlyRateProvided = true;
	}

}
