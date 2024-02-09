package com.dynatrace.dynapay.controller;

import com.dynatrace.model.DynaPay;
import com.dynatrace.dynapay.repository.ConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dynapay")
public class DynaPayController extends HardworkingController {
    @Value("${dynapay.bankcheck}")
    private String lambdaCall;
    private final String lambdaExpr = "lambda";

    @Autowired
    private ConfigRepository configRepository;
    private Logger logger = LoggerFactory.getLogger(DynaPayController.class);
    @Autowired
    private BankInfoController bankInfoController;


    // make a payment
    @PostMapping("")
    @Operation(summary = "Make a bank payment (i.e. payment gateway)")
    public DynaPay createBankPayment(@RequestBody DynaPay dynaPay) {
        simulateHardWork();
        simulateCrash();

        LambdaValidator bankValid = this.callLambda() ? bankInfoController.validateBankInformation() : LambdaValidator.NONE;
        final String paymentSucceed = "Payment succeeded";
        final String paymentFailed  = "Payment failed";


        switch (bankValid) {
            case OK:
                dynaPay.setSucceeded(true);
                dynaPay.setMessage(paymentSucceed);
                logger.info(paymentSucceed);
                break;
            case FAIL:
                dynaPay.setSucceeded(false);
                dynaPay.setMessage(paymentFailed);
                logger.error(paymentFailed);
                break;
            case NONE:
                double rand = Math.random();
                logger.info("Processing Payment... Rand = " + rand + " probability to fail = " + getPercentFailure() + "%");

                if (rand >= getPercentFailure() / 100.0) {
                    // successful payment
                    dynaPay.setSucceeded(true);
                    dynaPay.setMessage(paymentSucceed);
                    logger.info(paymentSucceed);
                } else {
                    dynaPay.setSucceeded(false);
                    dynaPay.setMessage(paymentFailed);
                    logger.error(paymentFailed);
                }
                break;
        }

        return dynaPay;
    }

    @Override
    public ConfigRepository getConfigRepository() {
        return configRepository;
    }

    private boolean callLambda() {
        return this.lambdaCall != null && this.lambdaCall.equals(lambdaExpr);
    }
}
