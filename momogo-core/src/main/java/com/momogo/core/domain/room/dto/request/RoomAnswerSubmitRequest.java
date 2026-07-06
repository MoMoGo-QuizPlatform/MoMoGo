package com.momogo.core.domain.room.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RoomAnswerSubmitRequest(
    @Valid
    @NotNull(message = "답안 목록은 필수입니다.")
    List<ProblemAnswerRequest> answers
) {

}
