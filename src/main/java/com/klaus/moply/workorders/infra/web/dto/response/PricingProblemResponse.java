package com.klaus.moply.workorders.infra.web.dto.response;

import java.net.URI;

public record PricingProblemResponse(URI type, String title, int status, String detail, URI instance, String code,
		PricingPreviewResponse pricingPreview) {
}
