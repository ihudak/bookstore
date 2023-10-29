package com.dynatrace.model;

public class Version implements Model {
    private String serviceId;
    private String ver;
    private String verDocker;
    private String date;
    private String status;
    private String message;
    private String agent;
    private String agentPreload;

    public Version() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Version(String serviceId, String ver, String verDocker, String date, String status, String message, String agent, String agentPreload) {
        this.serviceId = serviceId;
        this.ver = ver;
        this.verDocker = verDocker;
        this.date = date;
        this.status = status;
        this.message = message;
        this.agent = agent;
        this.agentPreload = agentPreload;
    }

    @Override
    public long getId() {
        return 0;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getVer() {
        return ver;
    }

    public void setVer(String ver) {
        this.ver = ver;
    }

    public String getVerDocker() {
        return verDocker;
    }

    public void setVerDocker(String verDocker) {
        this.ver = verDocker;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String getAgentPreload() {
        return switch (agentPreload) {
            case "true" -> "InImage";
            case "false" -> "OnDeploy";
            case "NONE" -> "NotInstrumented";
            default -> agentPreload;
        };
    }

    public void setAgentPreload(String agentPreload) {
        this.agentPreload = agentPreload;
    }
}
