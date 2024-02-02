package com.dynatrace.dynapay.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Component
public class BankInfoController {
    private final Logger logger = LoggerFactory.getLogger(BankInfoController.class);
    RestTemplate restTemplate = new RestTemplate();

    public LambdaValidator validateBankInformation() {
        try {
            String lambdaURL = Optional
                .ofNullable(
                    System.getenv("AWS_LAMBDA_URL")
                ).orElseThrow();

            List<String> bank_names = Arrays.asList(
            "Ward Holdings",
            "Connection Financial Group",
            "Eminence Trust",
            "Goldleaf Banks",
            "His Majesty Holdings",
            "Value Banks Inc.",
            "Concorde Financial Group",
            "Life Essence Bank",
            "Obsidian Trust Corp.",
            "Pursuit Bank Inc."
            );
            Random rand = new Random();
            String bank_name = bank_names.get(rand.nextInt(bank_names.size()));

            logger.info("Calling {} with bank name {}", lambdaURL,bank_name);
            HttpEntity<String> request = 
                new HttpEntity<String>("{\"used_bank\":\""+bank_name+ "\"}");
            ResponseEntity<String> response = restTemplate
                .postForEntity(lambdaURL,request, String.class);

            logger.info("AWS Lambda function response: {}", response);
            
            if(response.getStatusCode()==HttpStatus.OK) {
                return LambdaValidator.OK;
            }
            else {
                return LambdaValidator.FAIL;
            }

        } catch (Exception e) {
            logger.info("error occurred in lambda: {}",e.getMessage() );
            return LambdaValidator.NONE;
        }
    }

}
