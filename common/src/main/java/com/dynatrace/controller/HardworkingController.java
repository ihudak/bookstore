package com.dynatrace.controller;

import com.dynatrace.exception.CrashException;
import com.dynatrace.model.ConfigModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.lang.Math.sqrt;

public abstract class HardworkingController {
    public abstract JpaRepository getConfigRepository();
    protected Logger logger = LoggerFactory.getLogger(HardworkingController.class);

    protected boolean shouldWorkHard() {
        Optional<ConfigModel> config = getConfigRepository().findById("dt.work.hard");
        return config.isPresent() && config.get().isTurnedOn() && 10 < getMemPressureMb() && 100 < getCPUPressure();
    }

    @SuppressWarnings("unchecked")
    protected boolean shouldSimulateCrash() {
        Optional<ConfigModel> config = getConfigRepository().findById("dt.simulate.crash");
        if (config.isEmpty() || !config.get().isTurnedOn()) {
            return false;
        }

        /**
         * timeToCrash is fromMin/toMin/everyNhour
         * for instance, 15/22/4, means between 15 and 22 min every 4th hour of the day
         * 22/15/4 means the whole hour (every 4th still)
         * empty string or unparseable string means - crash regardless of time
         */
        String timeToCrash = config.get().getPropertyStr();
        if (!timeToCrash.isEmpty()) {
            String[] timesToParse = timeToCrash.split("/");
            int minFrom = -1, minTo = -1, hour = -1; // wrong numbers
            try {
                minFrom = Integer.parseInt(timesToParse[0]);
                minTo   = Integer.parseInt(timesToParse[1]);
                hour    = Integer.parseInt(timesToParse[2]);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException exception) {
                logger.info(exception.getMessage());
            }

            if (minFrom > minTo) {
                minFrom = 0;
                minTo = 59;
            }
            LocalDateTime now = LocalDateTime.now();
            int currMin = now.getMinute();
            int currHour = now.getHour();

            // the time to crash hasn't yet come
            if (minFrom > currMin) {
                return false;
            }
            // the time to crash has already passed
            if (minTo < currMin) {
                return false;
            }
            if (hour != 0 && currHour % hour != 0) {
                return false;
            }
        }

        /**
         * probability of crash
         */
        double perFail = config.get().getProbabilityFailure();
        if (perFail > 100.0 || perFail <= 0.0) {
            perFail = 100.0;
        } else if (perFail < 0.0) {
            perFail = 0.0;
        }

        double rand = Math.random();
        logger.info("Deciding about whether to crash... Rand = " + rand + " probability to crash = " + perFail + "%");

        return rand >= perFail / 100.0;
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

    protected void simulateCrash() {
        if (!shouldSimulateCrash()) {
            return;
        }
        throw new CrashException("Service is not available");
    }

    protected void simulateHardWork() {
        if (!shouldWorkHard()) {
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
}
