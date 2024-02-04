package com.dynatrace.ingest.controller;

import com.dynatrace.ingest.model.Version;
import com.dynatrace.ingest.repository.IngestSelfRepository;
import com.dynatrace.ingest.repository.versions.*;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@RestController
@RequestMapping("/api/v1/version")
public class VersionController {
    @Value("${application.version}")
    private String svcVer;
    @Value("${application.date}")
    private String svcDate;
    @Value("${docker.version}")
    private String svcVerDocker;
    @Value("${docker.agent.vendor}")
    private String dockerAgent;
    @Value("${docker.agent.preload}")
    private String dockerAgentPreload;

    private final long defaultTimeOut = 3500;

    @Autowired
    VersionRepository clientVersionRepository;
    @Autowired
    VersionRepository bookVersionRepository;
    @Autowired
    VersionRepository cartVersionRepository;
    @Autowired
    VersionRepository storageVersionRepository;
    @Autowired
    VersionRepository orderVersionRepository;
    @Autowired
    VersionRepository paymentVersionRepository;
    @Autowired
    VersionRepository dynapayVersionRepository;
    @Autowired
    VersionRepository ratingsVersionRepository;
    @Autowired
    IngestSelfRepository ingestSelfRepository;

    @GetMapping("")
    @Operation(summary = "Get versions of all services, their release dates and numbers of records in the DBs")
    public List<Version> getVersion() {
        List<Version> versions = new ArrayList<>();

        versions.add(new Version("ingest", svcVer, svcVerDocker, svcDate, getIngestStatus(), "Healthy", dockerAgent, dockerAgentPreload));

        HashMap<String, CompletableFuture<Version>> completableVersions = new HashMap<>();

        completableVersions.put(clientVersionRepository.getServiceName(),  clientVersionRepository.getVersion());
        completableVersions.put(bookVersionRepository.getServiceName(),    bookVersionRepository.getVersion());
        completableVersions.put(cartVersionRepository.getServiceName(),    cartVersionRepository.getVersion());
        completableVersions.put(storageVersionRepository.getServiceName(), storageVersionRepository.getVersion());
        completableVersions.put(orderVersionRepository.getServiceName(),   orderVersionRepository.getVersion());
        completableVersions.put(ratingsVersionRepository.getServiceName(), ratingsVersionRepository.getVersion());
        completableVersions.put(paymentVersionRepository.getServiceName(), paymentVersionRepository.getVersion());
        completableVersions.put(dynapayVersionRepository.getServiceName(), dynapayVersionRepository.getVersion());

        for(var ver: completableVersions.entrySet()) {
            try {
                versions.add(ver.getValue().get(defaultTimeOut, TimeUnit.MILLISECONDS));
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                Version v = new Version(ver.getKey(), exception.getMessage());
                v.setStatus(exception.getClass().toString());
                v.setMessageIfEmpty(exception.getClass().descriptorString());
                versions.add(v);
            }
        }

        return versions;
    }

    @GetMapping("/ingest")
    @Operation(summary = "Get version the Ingest Service only + Data Generation status")
    public Version getIngestVersion() {
        return new Version("ingest", svcVer, svcVerDocker, svcDate, getIngestStatus(), "Healthy", dockerAgent, dockerAgentPreload);
    }

    private String getIngestStatus() {
        String ingestStatus = IngestController.isIsWorking() ? "generation in progress" : "generation is off";
        try {
            ingestStatus = ingestSelfRepository.getStatus();
        } catch (RestClientException exception) {
            ingestStatus = exception.getMessage();
        }
        return ingestStatus;
    }
}
