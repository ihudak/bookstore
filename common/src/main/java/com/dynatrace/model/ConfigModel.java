package com.dynatrace.model;

public interface ConfigModel {
    String getId();
    String toString();
    void setId(String id);
    long getLoadCPU();
    void setLoadCPU(long loadCPU);
    long getLoadRAM();
    void setLoadRAM(long loadRAM);
    double getProbabilityFailure();
    void setProbabilityFailure(double propertyDouble);
    String getPropertyStr();
    void setPropertyStr(String propertyStr);
    boolean isTurnedOn();
    void setTurnedOn(boolean propertyBool);
}