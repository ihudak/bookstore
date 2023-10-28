package com.dynatrace.payments.model;

import com.dynatrace.model.ConfigModel;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.persistence.*;

@Entity
@Table(name="configs")
public class Config implements ConfigModel {
    @Id
    @Schema(name = "id", example = "dt.work.hard", requiredMode = Schema.RequiredMode.REQUIRED, description = "dt.work.hard - simulates CPU/RAM load; dt.simulate.crash - simulates service crashes")
    private String id;

    @Column(name="load_cpu", nullable = true)
    @Schema(name = "load_cpu", example = "500000", requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "for dt.work.hard only - how many loop cycles to make for each operation to simulate load")
    private long loadCPU;

    @Column(name="load_ram", nullable = true)
    @Schema(name = "load_ram", example = "64", requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "for dt.work.hard only - megabytes to allocate on each operation to simulate memory pressure")
    private long loadRAM;

    @Column(name="probab_fail", nullable = true)
    @Schema(name = "probab_fail", example = "75", requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "for dt.simulate.crash is the probability to crash, in %")
    private double probabilityFailure;

    @Column(name="property_str", nullable = true)
    @Schema(name = "property_str", example = "15/33/5", requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "for dt.simulate.crash can be a string like m1/m2/h, which means crash from m1 to m2 every h hour")
    private String propertyStr;

    @Column(name="turn_on", nullable = true)
    @Schema(name = "turn_on", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "enables or suppresses the config. Suppress is default")
    private boolean turnedOn;

    public Config() {
    }

    public Config(String id, long loadCPU, long loadRAM, double probabilityFailure, String propertyStr, boolean turnedOn) {
        this.id = id;
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
