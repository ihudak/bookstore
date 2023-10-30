package com.dynatrace.books.controller;

import com.dynatrace.model.Version;
import com.dynatrace.books.repository.BookRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/version")
public class VersionController {
    @Autowired
    private BookRepository bookRepository;
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

    @GetMapping("")
    @Operation(summary = "Get version, release date and number of records in the DB")
    public Version getVersion() {
        return new Version("books", svcVer, svcVerDocker, svcDate, "OK", "Count: " + bookRepository.count(), dockerAgent, dockerAgentPreload);
    }
}
