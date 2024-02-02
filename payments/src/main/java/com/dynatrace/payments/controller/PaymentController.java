package com.dynatrace.payments.controller;

import com.dynatrace.controller.HardworkingController;
import com.dynatrace.model.DynaPay;
import com.dynatrace.model.Payment;
import com.dynatrace.payments.repository.ConfigRepository;
import com.dynatrace.payments.repository.DynaPayRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController extends HardworkingController {
    @Autowired
    DynaPayRepository dynaPayRepository;
    @Autowired
    ConfigRepository configRepository;
    private Logger logger = LoggerFactory.getLogger(PaymentController.class);



    // make a payment
    @PostMapping("")
    @Operation(summary = "Forward the payment to the gateway (dynapay service)")
    public Payment createPayment(@RequestBody Payment payment) {
        DynaPay dynaPay = dynaPayRepository.submitPayment(payment);
        payment.setSucceeded(dynaPay.isSucceeded());
        payment.setMessage(dynaPay.getMessage());
        logger.info("Payment Processing Info: " + dynaPay.isSucceeded() + " message: " + dynaPay.getMessage());
        return payment;
    }


    @Override
    public ConfigRepository getConfigRepository() {
        return configRepository;
    }
}
