package com.klaus.moply.collaborators.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.application.usecase.CreateCollaborator;
import com.klaus.moply.collaborators.application.usecase.DeactivateCollaborator;
import com.klaus.moply.collaborators.application.usecase.FindAllCollaborators;
import com.klaus.moply.collaborators.application.usecase.FindCollaboratorById;
import com.klaus.moply.collaborators.application.usecase.FindEligibleCollaborators;
import com.klaus.moply.collaborators.application.usecase.UpdateCollaborator;

@Configuration
public class CollaboratorConfig {

	@Bean
	public CreateCollaborator createCollaborator(CollaboratorRepository repo) {
		return new CreateCollaborator(repo);
	}

	@Bean
	public FindCollaboratorById findCollaboratorById(CollaboratorRepository repo) {
		return new FindCollaboratorById(repo);
	}

	@Bean
	public FindAllCollaborators findAllCollaborators(CollaboratorRepository repo) {
		return new FindAllCollaborators(repo);
	}

	@Bean
	public FindEligibleCollaborators findEligibleCollaborators(CollaboratorRepository repo) {
		return new FindEligibleCollaborators(repo);
	}

	@Bean
	public UpdateCollaborator updateCollaborator(CollaboratorRepository repo) {
		return new UpdateCollaborator(repo);
	}

	@Bean
	public DeactivateCollaborator deactivateCollaborator(CollaboratorRepository repo) {
		return new DeactivateCollaborator(repo);
	}

}
