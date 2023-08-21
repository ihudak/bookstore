package com.dynatrace.ingest.repository.versions;

import com.dynatrace.ingest.model.Version;
import org.slf4j.Logger;
import org.springframework.web.client.RestTemplate;

public interface VersionRepository {
    String getBaseURL();
    String getServiceName();
    RestTemplate getRestTemplate();
    Logger getLogger();

    default Version getVersion() {
        getLogger().info("Getting version and status for service. URL: " + getBaseURL());
        try {
            Version version = getRestTemplate().getForObject(getBaseURL(), Version.class);
            version.setServiceId(getServiceName());
            if (version.isMessageEmpty()) {
                version.setMessage("Healthy");
            }
            return version;
        } catch (Exception exception) {
            getLogger().error(exception.getMessage());
            return new Version(getServiceName(), exception.getMessage());
        }
    }
}
