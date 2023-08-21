package com.dynatrace.carts.controller;

import com.dynatrace.exception.ResourceNotFoundException;
import com.dynatrace.carts.model.Config;
import com.dynatrace.carts.repository.ConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {
    @Autowired
    private ConfigRepository configRepository;

    // get all settings
    @GetMapping("")
    @Operation(summary = "Get the service configuration")
    public List<Config> getAllConfigs() {
        return configRepository.findAll();
    }

    // get a setting
    @GetMapping("/{id}")
    @Operation(summary = "Get a setting by its identifier")
    public Config getConfigById(@Parameter(name="id", description = "setting identifier: dt.work.hard; dt.simulate.crash", example = "dt.work.hard") @PathVariable String id) {
        Optional<Config> config = configRepository.findById(id);
        if (config.isEmpty()) {
            throw new ResourceNotFoundException("Config does not exist");
        }
        return config.get();
    }

    // create a setting
    @PostMapping("")
    @Operation(summary = "Set a configuration to the service")
    public Config createConfig(@RequestBody Config config) {
        return configRepository.save(config);
    }

    // update a config
    @PutMapping("/{id}")
    @Operation(summary = "Update a configuration by its identifier")
    public Config updateConfig(@Parameter(name="id", description = "setting identifier: dt.work.hard; dt.simulate.crash", example = "dt.work.hard") @PathVariable String id, @RequestBody Config config) {
        Optional<Config> configDb = configRepository.findById(id);
        if (configDb.isEmpty()) {
            throw new ResourceNotFoundException("Config does not exist");
        }
        return configRepository.save(config);
    }

    // delete a config
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a setting (change to default)")
    public void deleteConfig(@Parameter(name="id", description = "setting identifier: dt.work.hard; dt.simulate.crash", example = "dt.work.hard") @PathVariable String id) {
        configRepository.deleteById(id);
    }

    // delete all configs
    @DeleteMapping("/delete-all")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Delete all configs for the service (reset to defaults)")
    public void deleteAllBooks() {
        configRepository.deleteAll();
    }
}
