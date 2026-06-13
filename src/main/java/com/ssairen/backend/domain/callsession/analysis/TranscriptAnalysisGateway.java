package com.ssairen.backend.domain.callsession.analysis;

import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisCommand;
import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisResult;

public interface TranscriptAnalysisGateway {

    TranscriptAnalysisResult analyzeRest(TranscriptAnalysisCommand command);

    TranscriptAnalysisResult analyzeWebSocket(TranscriptAnalysisCommand command);
}
