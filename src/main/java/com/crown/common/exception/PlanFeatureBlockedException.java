package com.crown.common.exception;

import lombok.Getter;

@Getter
public class PlanFeatureBlockedException extends RuntimeException {

    private final String currentPlan;
    private final String requiredPlan;

    public PlanFeatureBlockedException(String message, String currentPlan, String requiredPlan) {
        super(message);
        this.currentPlan = currentPlan;
        this.requiredPlan = requiredPlan;
    }

    public PlanFeatureBlockedException(String message, String currentPlan) {
        this(message, currentPlan, "starter");
    }
}
