package com.dynatrace.ingest.model;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="settings")
public class Setting {
    @Id
    @Schema(name = "id", nullable = false, example = "qwh76554", requiredMode = Schema.RequiredMode.REQUIRED, description = "Dynatrace Tenant ID")
    private String id;

    @Column(name="env", nullable = false)
    @Schema(name = "env", nullable = false, example = "dev", requiredMode = Schema.RequiredMode.REQUIRED, description = "dev|sprint")
    private String env;

    @Column(name="custom_url", nullable = false)
    @Schema(name = "customUrl", nullable = false, example = "bookstore.dynatracelabs.internal", requiredMode = Schema.RequiredMode.REQUIRED, description = "root url for the cluster")
    private String customUrl;

    @Column(name="active", nullable = false)
    @Schema(name = "active", nullable = false, example = "true", requiredMode = Schema.RequiredMode.REQUIRED, description = "true if the configuration is active. only one or zero must be active. If none is active, preconfigured URL will be used")
    private boolean active;

    public Setting() {
    }

    public Setting(String id, String env, String customUrl, boolean active) {
        this.id = id;
        this.env = env;
        this.customUrl = customUrl;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getCustomUrl() {
        return customUrl;
    }

    public void setCustomUrl(String customUrl) {
        this.customUrl = customUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
