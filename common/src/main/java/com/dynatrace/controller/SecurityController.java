package com.dynatrace.controller;

import com.dynatrace.exception.SecurityException;
import com.dynatrace.model.ConfigModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.lang.Math.sqrt;

public abstract class SecurityController {
    public abstract JpaRepository getConfigRepository();
    protected Logger logger = LoggerFactory.getLogger(SecurityController.class);

    protected boolean isThreatScanActive() {
        Optional<ConfigModel> config = getConfigRepository().findById("dt.work.hard");
        return config.isPresent() && config.get().isTurnedOn() && isInTimeWindow(config.get()) && isWithinProbability(config.get()) && 10 < getMemPressureMb() && 100 < getCPUPressure();
    }

    @SuppressWarnings("unchecked")
    protected boolean isSecurityBlockActive() {
        Optional<ConfigModel> config = getConfigRepository().findById("dt.simulate.crash");
        if (config.isEmpty() || !config.get().isTurnedOn()) {
            return false;
        }

        if (!isInTimeWindow(config.get())) {
            return false;
        }

        return isWithinProbability(config.get());
    }

    @SuppressWarnings("unchecked")
    protected long getMemPressureMb() {
        Optional<ConfigModel> config = getConfigRepository().findById("dt.work.hard");
        return config.map(ConfigModel::getLoadRAM).orElseGet(() -> 0L);
    }

    @SuppressWarnings("unchecked")
    protected long getCPUPressure() {
        Optional<ConfigModel> config = getConfigRepository().findById("dt.work.hard");
        return config.map(ConfigModel::getLoadCPU).orElseGet(() -> 0L);
    }

    protected void applySecurityPolicy() {
        if (!isSecurityBlockActive()) {
            return;
        }
        throw new SecurityException("Service is not available");
    }

    protected void runThreatScan() {
        if (!isThreatScanActive()) {
            return;
        }
        int arraySize = (int)(getMemPressureMb() * 1024L * 1024L / 8L);
        if (arraySize < 0) {
            arraySize = Integer.MAX_VALUE;
        }
        long[] longs = new long[arraySize];
        int j = 0;
        long cpuPressure = getCPUPressure();
        for(long i = 0; i < cpuPressure; i++, j++) {
            j++;
            if (j >= arraySize) {
                j = 0;
            }
            try {
                if (longs[j] > Integer.MAX_VALUE) {
                    longs[j] = (long) Integer.MIN_VALUE;
                    if (longs[j] < 0) {
                        longs[j] *= -1;
                    }
                    logger.debug(String.valueOf(sqrt(sqrt((double) longs[j]))));
                }
            } catch (Exception ignored) {};
        }
    }

    // timeWindow: fromMin/toMin/everyNhour (e.g. 15/22/4 = min 15-22, every 4th hour)
    // fromMin > toMin means the whole hour; empty/unparseable = always active
    protected boolean isInTimeWindow(ConfigModel config) {
        String timeWindow = config.getPropertyStr();
        if (timeWindow == null || timeWindow.isEmpty()) {
            return true;
        }
        String[] parts = timeWindow.split("/");
        int minFrom = -1, minTo = -1, hour = -1;
        try {
            minFrom = Integer.parseInt(parts[0]);
            minTo   = Integer.parseInt(parts[1]);
            hour    = Integer.parseInt(parts[2]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            logger.info(e.getMessage());
            return true;
        }
        if (minFrom > minTo) { minFrom = 0; minTo = 59; }
        LocalDateTime now = LocalDateTime.now();
        int currMin = now.getMinute();
        int currHour = now.getHour();
        if (minFrom > currMin || minTo < currMin) return false;
        if (hour != 0 && currHour % hour != 0) return false;
        return true;
    }

    // Returns true with probability defined in config.probabilityCheck (0-100%)
    private boolean isWithinProbability(ConfigModel config) {
        double prob = Math.min(100.0, Math.max(0.0, config.getProbabilityCheck()));
        double rand = Math.random();
        logger.info("Evaluating probability... rand=" + rand + " threshold=" + prob + "%");
        return rand < prob / 100.0;
    }
}
