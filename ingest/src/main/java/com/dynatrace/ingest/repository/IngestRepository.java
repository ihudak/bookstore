package com.dynatrace.ingest.repository;

import com.dynatrace.ingest.model.Model;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestTemplate;

public interface IngestRepository {
    String getBaseURL();
    RestTemplate getRestTemplate();
    Model[] getAll();
    void create(@Nullable Object model);
    void create();
    void update(Object model);
    void clearModel();
    default void deleteAll() {
        String urlBuilder = getBaseURL() + "/delete-all";
        try {
            getRestTemplate().delete(urlBuilder);
        } catch (Exception ignore){}
        clearModel();
    }
    default void delete(long id) {
        String urlBuilder = getBaseURL() + "/" + id;
        try {
            getRestTemplate().delete(urlBuilder);
        } catch (Exception ignore){}
    }
}
