package com.dynatrace.ingest.controller;

import com.dynatrace.exception.ResourceNotFoundException;
import com.dynatrace.ingest.model.Setting;
import com.dynatrace.ingest.repository.SettingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingController {
    @Autowired
    private SettingRepository settingRepository;

    private final Logger logger = LoggerFactory.getLogger(SettingController.class);

    @GetMapping("")
    @Operation(summary = "Get all settings for the UI application")
    public List<Setting> getAllSettings() {
        return settingRepository.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Setting by ID")
    public ResponseEntity<Setting> getSettingByID(@PathVariable String id) {
        Setting setting = settingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Setting does not exist"));
        if (setting == null) {
            logger.info("Retrieving Setting: no setting found for id " + id);
        } else {
            logger.info("Retrieving Setting: " + setting.getId());
        }
        return ResponseEntity.ok(setting);
    }

    @PostMapping("")
    @Operation(summary = "Create a new Setting for the UI app")
    public Setting createSetting(@RequestBody Setting setting) {
        logger.info("Creating setting with id: " + setting.getId());
        return settingRepository.save(setting);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a Setting for the UI app")
    public Setting updateSetting(@Parameter(name="id", description = "id") @PathVariable String id, @RequestBody Setting setting) {
        logger.info("Updating setting: " + id);
        Optional<Setting> settingDb = settingRepository.findById(id);
        if (settingDb.isEmpty()) {
            throw new ResourceNotFoundException("Setting does not exist");
        }
        return settingRepository.save(setting);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Setting for the UI app")
    public void deleteSetting(@Parameter(name="id", description = "id") @PathVariable String id) {
        logger.info("Deleting setting: " + id);
        settingRepository.deleteById(id);
    }
}
