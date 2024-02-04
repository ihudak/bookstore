package com.dynatrace.ingest.repository.configs;

import com.dynatrace.exception.BadRequestException;
import com.dynatrace.ingest.model.Config;
import org.slf4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ConfigRepository {
    String getBaseURL();
    String getServiceName();
    RestTemplate getRestTemplate();
    Logger getLogger();

    default Config getConfigByID(String id) {
        String urlBuilder = getBaseURL() + "/" + id;
        getLogger().info("Getting config " + id + " for service. URL: " + urlBuilder);
        try {
            Config config = getRestTemplate().getForObject(urlBuilder, Config.class);
            assert config != null;
            config.setServiceId(getServiceName());
            return config;
        } catch (Exception exception) {
            getLogger().error(exception.getMessage());
            throw new BadRequestException(exception.getMessage());
        }
    }

    @Async
    default CompletableFuture<List<Config>> getConfigsForService() {
        getLogger().info("Getting all configs for service. URL: " + getBaseURL());
        try {
            Config[] configs = getRestTemplate().getForObject(getBaseURL(), Config[].class);
            if (configs != null) {
                for (Config config: configs) {
                    config.setServiceId(this.getServiceName());
                }
            }
            assert configs != null;
            return CompletableFuture.completedFuture(Arrays.asList(configs));
        } catch (Exception exception) {
            getLogger().error(exception.getMessage());
            // make a config that tells about error
            Config config = new Config();
            config.setId("N/A");
            config.setServiceId(this.getServiceName());
            config.setPropertyStr("Not Available");
            return CompletableFuture.completedFuture(List.of(config));
        }
    }

    @Async
    default CompletableFuture<Config> createOrUpdate(@NonNull Config config) {
        String urlBuilder = getBaseURL();
        getLogger().info("Creating/Updating config " + config.getId() + " for service. URL: " + urlBuilder);
        try {
            Config resultConfig = getRestTemplate().postForObject(urlBuilder, config, Config.class);
            if (resultConfig != null) {
                resultConfig.setServiceId(this.getServiceName());
            }
            return CompletableFuture.completedFuture(resultConfig);
        } catch (Exception exception) {
            getLogger().error(exception.getMessage());
            throw new BadRequestException(exception.getMessage());
        }
    }

    @Async
    default void delete(String id) {
        String urlBuilder = getBaseURL() + "/" + id;
        getLogger().info("Deleting config for service. URL: " + urlBuilder);
        try {
            getRestTemplate().delete(urlBuilder);
        } catch (Exception exception) {
            getLogger().error(exception.getMessage());
            throw new BadRequestException(exception.getMessage());
        }
    }

    @Async
    default void delete_all() {
        String urlBuilder = getBaseURL() + "/delete-all";
        getLogger().info("Deleting all configs for service. URL: " + urlBuilder);
        try {
            getRestTemplate().delete(urlBuilder);
        } catch (Exception exception) {
            getLogger().error(exception.getMessage());
            throw new BadRequestException(exception.getMessage());
        }
    }
}
