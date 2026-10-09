package com.klaus.moply.collaborators.application.usecase.dto;

import com.klaus.moply.shared.application.pagination.PageQuery;

public record FindAllCollaboratorsFilter(Boolean active, PageQuery page) {
}
