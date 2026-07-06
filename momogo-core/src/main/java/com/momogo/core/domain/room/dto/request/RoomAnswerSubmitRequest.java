package com.momogo.core.domain.room.dto.request;

import jakarta.validation.Valid;
import java.util.List;

public record RoomAnswerSubmitRequest(
    @Valid
    List<ProblemAnswerRequest> answers
) {

}
