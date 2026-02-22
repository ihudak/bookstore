package com.dynatrace.dynapay.controller;

import com.dynatrace.dynapay.model.Config;
import java.util.Optional;

public abstract class SecurityController extends com.dynatrace.controller.SecurityController {

    @SuppressWarnings("unchecked")
    protected double getPaymentRiskThreshold() {
        Optional<Config> config = getConfigRepository().findById("dt.failure.payment.percent");
        if (config.isEmpty() || !config.get().isTurnedOn()) {
            return 0.0;
        }
        double perFail = config.get().getProbabilityFailure();
        if (perFail > 100.0) {
            return 100.0;
        } else if (perFail < 0.0) {
            return 0.0;
        }
        return perFail;
    }

}
