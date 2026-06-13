package com.ssairen.backend.domain.callsession.analysis.dto;

import com.ssairen.backend.domain.casefile.entity.PhishingType;
import java.util.List;

public record TranscriptAnalysisResult(
        int riskScore,
        PhishingType phishingType,
        String aiSummary,
        List<String> keywords,
        String provider
) {
}
