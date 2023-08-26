package com.dynatrace.ingest.model;

import io.swagger.v3.oas.annotations.media.Schema;

public class Version implements Model {
    @Schema(name = "serviceId", example = "books", requiredMode = Schema.RequiredMode.AUTO, description = "books/clients/carts/storage/ratings/orders/payments/dynapay")
    private String serviceId;
    @Schema(name = "ver", example = "1.0.2", requiredMode = Schema.RequiredMode.AUTO, description = "the version of the service")
    private String ver;
    @Schema(name = "verDocker", example = "1.0.0", requiredMode = Schema.RequiredMode.AUTO, description = "the version of the Docker Config")
    private String verDocker;
    @Schema(name = "date", example = "Jul 12, 2023", requiredMode = Schema.RequiredMode.AUTO, description = "the release date")
    private String date;
    @Schema(name = "status", example = "Healthy", requiredMode = Schema.RequiredMode.AUTO, description = "the status of the service or number of records in its DB")
    private String status;
    @Schema(name = "message", example = "I am fine! :)", requiredMode = Schema.RequiredMode.AUTO, description = "message from the service")
    private String message;

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

    public Version(String serviceId, String ver, String verDocker, String date, String status, String message) {
        this.serviceId = serviceId;
        this.ver = ver;
        this.verDocker = verDocker;
        this.date = date;
        this.status = status;
        this.message = message;
    }

    public Version(String serviceId, String message) {
        this.serviceId = serviceId;
        this.message   = message;
        this.ver = "N/A";
        this.verDocker = "N/A";
        this.date = "N/A";
        this.status = "N/A";
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
        this.verDocker = verDocker;
    }

    public String getMessage() {
        return message;
    }

    public boolean isMessageEmpty() {
        return this.message == null || this.message.isEmpty();
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
