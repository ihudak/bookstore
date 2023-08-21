package com.dynatrace.payments.repository;

import com.dynatrace.exception.PaymentException;
import com.dynatrace.model.DynaPay;
import com.dynatrace.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

@Repository
public class DynaPayRepository {
    @Value("${http.service.dynapay}")
    private String dynaPayBaseURL;
    private RestTemplate restTemplate;

    public DynaPayRepository() {
        restTemplate = new RestTemplate();
    }
    private Logger logger = LoggerFactory.getLogger(DynaPayRepository.class);



    public DynaPay submitPayment(@NonNull Payment payment) {
        String urlBuilder = dynaPayBaseURL;
        DynaPay dynaPay = restTemplate.postForObject(urlBuilder, payment, DynaPay.class);
        if (dynaPay == null || !dynaPay.isSucceeded()) {
            PaymentException ex = new PaymentException("Purchase was rejected: " + dynaPay.getMessage());
            logger.error(ex.getMessage());
            throw ex;
        }
        return dynaPay;
    }
}
