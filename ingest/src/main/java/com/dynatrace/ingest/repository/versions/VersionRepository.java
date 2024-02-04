package com.dynatrace.ingest.repository.versions;

import com.dynatrace.ingest.model.Version;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestTemplate;
import java.util.concurrent.CompletableFuture;

public interface VersionRepository {
    String getBaseURL();
    String getServiceName();
    RestTemplate getRestTemplate();
    Logger getLogger();

    @Async
    default CompletableFuture<Version> getVersion() {
        getLogger().info(getServiceName() + ": Getting version and status. URL: " + getBaseURL());
        try {
            Version version = getRestTemplate().getForObject(getBaseURL(), Version.class);
            version.setServiceId(getServiceName());
            version.setMessageIfEmpty("Healthy");
            return CompletableFuture.completedFuture(version);
        } catch (Exception exception) {
            getLogger().error(exception.getMessage());
            return CompletableFuture.completedFuture(new Version(getServiceName(), exception.getMessage()));
        }
    }
}
