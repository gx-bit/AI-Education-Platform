package com.edu.recommendation.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BehaviorRequest {
    @NotNull private Long courseId;
    @NotBlank @Pattern(regexp = "impression|click|favorite|order|purchase|start_learning|progress|complete|rating|search")
    private String behaviorType;
    private BigDecimal behaviorValue;
    private String sessionId;
    private String requestId;
    private String contextJson;
}
