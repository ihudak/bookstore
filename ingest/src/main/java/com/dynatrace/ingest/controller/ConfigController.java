package com.dynatrace.ingest.controller;

import com.dynatrace.exception.BadRequestException;
import com.dynatrace.ingest.model.Config;
import com.dynatrace.ingest.repository.configs.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {
    @Autowired
    private ClientConfigRepository clientConfigRepository;
    @Autowired
    private BookConfigRepository bookConfigRepository;
    @Autowired
    private CartConfigRepository cartConfigRepository;
    @Autowired
    private StorageConfigRepository storageConfigRepository;
    @Autowired
    private OrderConfigRepository orderConfigRepository;
    @Autowired
    private PaymentConfigRepository paymentConfigRepository;
    @Autowired
    private DynapayConfigRepository dynapayConfigRepository;
    @Autowired
    private RatingConfigRepository ratingConfigRepository;

    private final Logger logger = LoggerFactory.getLogger(ConfigController.class);

    @GetMapping("")
    @Operation(summary = "Get all configs from all services")
    public Map<String, List<Config>> getConfigsAllServices() {
        HashMap<String, List<Config>> allConfigs = new HashMap<>();
        String[] services = { "clients", "books", "carts", "storage", "ratings", "orders", "payments", "dynapay" };

        for(String service: services) {
            allConfigs.put(service, this.configsOfService(service));
        }
        return allConfigs;
    }

    // get all settings
    @GetMapping("/{serviceId}")
    @Operation(summary = "Get the configuration for the given service")
    public List<Config> getAllConfigs(
            @Parameter(name="serviceId", description = "service identifier: clients, books, carts, storage, ratings, orders, payments, dynapay", example = "orders") @PathVariable String serviceId) {
        return this.configsOfService(serviceId);
    }

    private List<Config> configsOfService(String serviceId) {
        ConfigRepository configRepository = getConfigRepoByServiceId(serviceId);
        if (configRepository == null) {
            BadRequestException badRequestException = new BadRequestException("Config is not supported " + serviceId);
            logger.error(badRequestException.getMessage());
            throw badRequestException;
        }
        return List.of(configRepository.getConfigsForService());
    }

    // get a setting
    @GetMapping("/{serviceId}/{id}")
    @Operation(summary = "Get a setting for the given service by its serviceID and setting identifier")
    public Config getConfigById(
            @Parameter(name="serviceId", description = "service identifier: clients, books, carts, storage, ratings, orders, payments, dynapay", example = "orders") @PathVariable String serviceId,
            @Parameter(name="id", description = "setting identifier: dt.work.hard; dt.simulate.crash; dt.failure.payment.percent", example = "dt.work.hard") @PathVariable String id) {
        ConfigRepository configRepository = getConfigRepoByServiceId(serviceId);
        if (configRepository == null) {
            BadRequestException badRequestException = new BadRequestException("Config is not supported " + serviceId);
            logger.error(badRequestException.getMessage());
            throw badRequestException;
        }
        return configRepository.getConfigByID(id);
    }

    // create a setting
    @PostMapping("/{serviceId}")
    @Operation(summary = "Set a configuration for the given service")
    public Config createConfig(
            @Parameter(name="serviceId", description = "service identifier: clients, books, carts, storage, ratings, orders, payments, dynapay", example = "orders") @PathVariable String serviceId,
            @RequestBody Config config) {
        ConfigRepository configRepository = getConfigRepoByServiceId(serviceId);
        if (configRepository == null) {
            BadRequestException badRequestException = new BadRequestException("Config is not supported " + serviceId);
            logger.error(badRequestException.getMessage());
            throw badRequestException;
        }
        return configRepository.createOrUpdate(config);
    }

    @PostMapping("")
    @Operation(summary = "Set the same config to all services")
    public Map<String, Config> setConfigsAllServices(@RequestBody Config config) {
        HashMap<String, Config> allConfigs = new HashMap<>();
        String[] services = { "clients", "books", "carts", "storage", "ratings", "orders", "payments", "dynapay" };

        for(String service: services) {
            ConfigRepository configRepository = getConfigRepoByServiceId(service);
            if (configRepository != null) {
                allConfigs.put(service, configRepository.createOrUpdate(config));
            }
        }
        return allConfigs;
    }

    // update a config
    @PutMapping("/{serviceId}/{id}")
    @Operation(summary = "Update a configuration for the given service by its serviceID and setting identifier")
    public Config updateConfig(
            @Parameter(name="serviceId", description = "service identifier: clients, books, carts, storage, ratings, orders, payments, dynapay", example = "orders") @PathVariable String serviceId,
            @Parameter(name="id", description = "setting identifier: dt.work.hard; dt.simulate.crash; dt.failure.payment.percent", example = "dt.work.hard") @PathVariable String id,
            @RequestBody Config config) {
        ConfigRepository configRepository = getConfigRepoByServiceId(serviceId);
        if (configRepository == null) {
            BadRequestException badRequestException = new BadRequestException("Config is not supported " + serviceId);
            logger.error(badRequestException.getMessage());
            throw badRequestException;
        }
        if (!id.equals(config.getId())) {
            BadRequestException badRequestException = new BadRequestException("Config Id " + config.getId() + " does not match the requested " + id);
            logger.error(badRequestException.getMessage());
            throw badRequestException;
        }
        return configRepository.createOrUpdate(config);
    }

    // delete a config
    @DeleteMapping("/{serviceId}/{id}")
    @Operation(summary = "Delete a setting (change to default) for the given service by its serviceID and setting identifier")
    public void deleteConfig(
            @Parameter(name="serviceId", description = "service identifier: clients, books, carts, storage, ratings, orders, payments, dynapay", example = "orders") @PathVariable String serviceId,
            @Parameter(name="id", description = "setting identifier: dt.work.hard; dt.simulate.crash; dt.failure.payment.percent", example = "dt.work.hard") @PathVariable String id) {
        ConfigRepository configRepository = getConfigRepoByServiceId(serviceId);
        if (configRepository == null) {
            BadRequestException badRequestException = new BadRequestException("Config is not supported " + serviceId);
            logger.error(badRequestException.getMessage());
            throw badRequestException;
        }
        configRepository.delete(id);
    }

    // delete all configs for a service
    @DeleteMapping("/{serviceId}/delete-all")
    @Operation(summary = "Delete all settings (change to defaults) for the given service by its serviceID")
    public void deleteAllConfigsForService(
            @Parameter(name="serviceId", description = "service identifier: clients, books, carts, storage, ratings, orders, payments, dynapay", example = "orders") @PathVariable String serviceId) {
        ConfigRepository configRepository = getConfigRepoByServiceId(serviceId);
        if (configRepository == null) {
            BadRequestException badRequestException = new BadRequestException("Config is not supported " + serviceId);
            logger.error(badRequestException.getMessage());
            throw badRequestException;
        }
        configRepository.delete_all();
    }

    // delete all configs for all services
    @DeleteMapping("/delete-all")
    @Operation(summary = "Deletes all configs for all services (reset all settings to defaults)")
    public void deleteAllConfigs() {
        dynapayConfigRepository.delete_all();
        paymentConfigRepository.delete_all();
        orderConfigRepository.delete_all();
        ratingConfigRepository.delete_all();
        storageConfigRepository.delete_all();
        cartConfigRepository.delete_all();
        clientConfigRepository.delete_all();
        bookConfigRepository.delete_all();
    }

    private ConfigRepository getConfigRepoByServiceId(@NonNull String serviceId) {
        return switch (serviceId) {
            case "clients", "client"                               -> clientConfigRepository;
            case "books", "book"                                   -> bookConfigRepository;
            case "carts", "cart"                                   -> cartConfigRepository;
            case "storage", "inventory", "storages", "inventories" -> storageConfigRepository;
            case "orders", "order"                                 -> orderConfigRepository;
            case "payments", "payment"                             -> paymentConfigRepository;
            case "dynapay", "superpay", "dynapays", "superpays"    -> dynapayConfigRepository;
            case "ratings", "rating"                               -> ratingConfigRepository;
            default -> null;
        };
    }
}
