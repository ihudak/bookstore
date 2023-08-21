package com.dynatrace.orders.repository;

import com.dynatrace.exception.PaymentException;
import com.dynatrace.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Repository
public class PaymentRepository {
    @Value("${http.service.payment}")
    private String paymentBaseURL;
    private RestTemplate restTemplate;
    private Logger logger = LoggerFactory.getLogger(PaymentRepository.class);

    public PaymentRepository() {
        restTemplate = new RestTemplate();
    }



    public Payment submitPayment(@NonNull Payment payment) {
        String urlBuilder = paymentBaseURL;
        Payment paymentRes = null;
        logger.info("Making Payment");
        logger.info(urlBuilder);
        try {
            paymentRes = restTemplate.postForObject(urlBuilder, payment, Payment.class);
        } catch (HttpClientErrorException exception) {
            PaymentException ex = new PaymentException("Payment rejected: " + exception.getMessage());
            logger.error(ex.getMessage());
            throw ex;
        }
        if (paymentRes == null) {
            logger.error("Purchase filed - response was null");
            throw new PaymentException("Purchase failed");
        }
        return paymentRes;
    }
}
