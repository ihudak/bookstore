package com.dynatrace.ingest.controller;

import com.dynatrace.exception.ResourceNotFoundException;
import com.dynatrace.ingest.model.Setting;
import com.dynatrace.ingest.repository.SettingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @GetMapping("")
    @Operation(summary = "Get all settings for the UI application")
    public List<Setting> getAllSettings() {
        return settingRepository.findAll();
    }

    @GetMapping("/active")
    @Operation(summary = "Get active Setting for the UI app")
    public Setting getActiveSetting() {
        return settingRepository.findOneByActive(true);
    }

    @GetMapping("/tenant/{id}")
    @Operation(summary = "Get Setting by TenantID")
    public ResponseEntity<Setting> getSettingByTenantID(@PathVariable String id) {
        Setting setting = settingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Setting does not exist"));
        return ResponseEntity.ok(setting);
    }

    @PostMapping("")
    @Operation(summary = "Create a new Setting for the UI app")
    public Setting createSetting(@RequestBody Setting setting) {
        return settingRepository.save(setting);
    }

    @PostMapping("/{id}")
    @Operation(summary = "Activate setting to the UI (previously active setting shall be deactivated")
    public Setting activateSetting(@Parameter(name="id", description = "tenantId") @PathVariable String id) {
        Optional<Setting> settingDb = settingRepository.findById(id);
        if (settingDb.isEmpty()) {
            throw new ResourceNotFoundException("Setting does not exist");
        }
        settingRepository.deactivateAllSettings();
        Setting setting = settingDb.get();
        setting.setActive(true);
        return setting;
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a Setting for the UI app")
    public Setting updateSetting(@Parameter(name="id", description = "tenantId") @PathVariable String id, @RequestBody Setting setting) {
        Optional<Setting> settingDb = settingRepository.findById(id);
        if (settingDb.isEmpty()) {
            throw new ResourceNotFoundException("Setting does not exist");
        }
        return settingRepository.save(setting);
    }

    @DeleteMapping("/deactivate-all")
    @Operation(summary = "Deactivate all custom settings")
    public void deactivateAllSettings() {
        settingRepository.deactivateAllSettings();
    }

    @DeleteMapping("/tenant/{id}")
    @Operation(summary = "Delete a Setting for the UI app")
    public void deleteSetting(@Parameter(name="id", description = "tenantId") @PathVariable String id) {
        settingRepository.deleteById(id);
    }

    @DeleteMapping("/delete-all")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Delete all Settings for the UI app")
    public void deleteAllSettings() {
        settingRepository.deleteAll();
    }
}
