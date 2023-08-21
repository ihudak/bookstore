package com.dynatrace.dynapay.controller;

import com.dynatrace.model.DynaPay;
import com.dynatrace.dynapay.repository.ConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dynapay")
public class DynaPayController extends HardworkingController {
    @Autowired
    private ConfigRepository configRepository;
    private Logger logger = LoggerFactory.getLogger(DynaPayController.class);


    // make a payment
    @PostMapping("")
    @Operation(summary = "Make a bank payment (i.e. payment gateway)")
    public DynaPay createBankPayment(@RequestBody DynaPay dynaPay) {
        simulateHardWork();
        simulateCrash();

        double rand = Math.random();
        logger.info("Processing Payment... Rand = " + rand + " probability to fail = " + getPercentFailure());

        if (rand >= getPercentFailure() / 100.0) {
            // successful payment
            dynaPay.setSucceeded(true);
            dynaPay.setMessage("Payment succeeded");
            logger.info("Payment succeeded");
        } else {
            dynaPay.setSucceeded(false);
            dynaPay.setMessage("Payment failed");
            logger.error("Payment failed");
        }

        return dynaPay;
    }

    @Override
    public ConfigRepository getConfigRepository() {
        return configRepository;
    }
}
