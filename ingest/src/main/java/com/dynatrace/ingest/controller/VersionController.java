package com.dynatrace.ingest.controller;

import com.dynatrace.ingest.model.Version;
import com.dynatrace.ingest.repository.versions.*;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api/v1/version")
public class VersionController {
    @Value("${application.version}")
    private String svcVer;
    @Value("${application.date}")
    private String svcDate;

    @Autowired
    ClientVersionRepository clientVersionRepository;
    @Autowired
    BookVersionRepository bookVersionRepository;
    @Autowired
    CartVersionRepository cartVersionRepository;
    @Autowired
    StorageVersionRepository storageVersionRepository;
    @Autowired
    OrderVersionRepository orderVersionRepository;
    @Autowired
    PaymentVersionRepository paymentVersionRepository;
    @Autowired
    DynapayVersionRepository dynapayVersionRepository;
    @Autowired
    RatingsVersionRepository ratingsVersionRepository;

    @GetMapping("")
    @Operation(summary = "Get versions of all services, their release dates and numbers of records in the DBs")
    public List<Version> getVersion() {
        List<Version> versions = new ArrayList<>();
        versions.add(new Version("ingest", svcVer, svcDate, (IngestController.isIsWorking() ? "generation in progress" : "generation is off"), "Healthy"));

        versions.add(clientVersionRepository.getVersion());
        versions.add(bookVersionRepository.getVersion());
        versions.add(cartVersionRepository.getVersion());
        versions.add(storageVersionRepository.getVersion());
        versions.add(orderVersionRepository.getVersion());
        versions.add(paymentVersionRepository.getVersion());
        versions.add(dynapayVersionRepository.getVersion());
        versions.add(ratingsVersionRepository.getVersion());

        return versions;
    }
}
