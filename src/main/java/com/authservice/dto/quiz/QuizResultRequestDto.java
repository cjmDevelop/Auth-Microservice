package com.authservice.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for submitting a quiz result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultRequestDto {

    private String quizDomain;        // e.g., "1.1", "2.3", etc.
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Integer timeTakenSeconds;
    private Boolean isTimed;
}
