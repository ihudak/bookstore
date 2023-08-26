package com.dynatrace.dynapay.controller;

import com.dynatrace.model.Version;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/version")
public class VersionController {
    @Value("${application.version}")
    private String svcVer;
    @Value("${application.date}")
    private String svcDate;
    @Value("${docker.version}")
    private String svcVerDocker;

    @GetMapping("")
    @Operation(summary = "Get version, release date")
    public Version getVersion() {
        return new Version("dynapay", svcVer, svcVerDocker, svcDate, "OK", "Healthy");
    }
}
