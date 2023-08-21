package com.dynatrace.ingest.model;

import io.swagger.v3.oas.annotations.media.Schema;

public class Config {
    @Schema(name = "id", example = "dt.work.hard", requiredMode = Schema.RequiredMode.REQUIRED, description = "dt.work.hard - simulates CPU/RAM load; dt.simulate.crash - simulates service crashes; dt.failure.payment.percent (dynapay only) - cause payments to fail")
    private String id;
    @Schema(name = "serviceId", example = "books", requiredMode = Schema.RequiredMode.REQUIRED, description = "Service to proxy-config: clients, books, carts, storage, orders, ratings, payments, dynapay")
    private String serviceId;
    @Schema(name = "load_cpu", example = "500000", requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "for dt.work.hard only - how many loop cycles to make for each operation to simulate load")
    private long loadCPU;
    @Schema(name = "load_ram", example = "64", requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "for dt.work.hard only - megabytes to allocate on each operation to simulate memory pressure")
    private long loadRAM;
    @Schema(name = "probab_fail", example = "75", requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "for dt.failure.payment.percent setting on dynapay service only. Causes payment failures (% of payments to fail)")
    private double probabilityFailure;
    @Schema(name = "property_str", example = "", requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Has no effect")
    private String propertyStr;

    @Schema(name = "turn_on", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "enables or suppresses the config. Suppress is default")
    private boolean turnedOn;

    public Config() {
    }

    public Config(String id, String serviceId, long loadCPU, long loadRAM, double probabilityFailure, String propertyStr, boolean turnedOn) {
        this.id = id;
        this.serviceId = serviceId;
        this.loadCPU = loadCPU;
        this.loadRAM = loadRAM;
        this.probabilityFailure = probabilityFailure;
        this.propertyStr = propertyStr;
        this.turnedOn = turnedOn;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public long getLoadCPU() {
        return loadCPU;
    }

    public void setLoadCPU(long loadCPU) {
        this.loadCPU = loadCPU;
    }

    public long getLoadRAM() {
        return loadRAM;
    }

    public void setLoadRAM(long loadRAM) {
        this.loadRAM = loadRAM;
    }
    public double getProbabilityFailure() {
        return probabilityFailure;
    }

    public void setProbabilityFailure(double propertyDouble) {
        this.probabilityFailure = propertyDouble;
    }

    public String getPropertyStr() {
        return propertyStr;
    }

    public void setPropertyStr(String propertyStr) {
        this.propertyStr = propertyStr;
    }

    public boolean isTurnedOn() {
        return turnedOn;
    }

    public void setTurnedOn(boolean propertyBool) {
        this.turnedOn = propertyBool;
    }
}
