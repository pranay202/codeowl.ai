package codeOwl.backend.services.ai;

import java.util.List;

import codeOwl.backend.dto.CitationDto;

public record RetrievedContext(
        List<CitationDto> citations,
        String contextText) {
}
