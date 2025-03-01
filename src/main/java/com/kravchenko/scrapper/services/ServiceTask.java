package com.kravchenko.scrapper.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

@AllArgsConstructor
public class ServiceTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceTask.class);

    @NonNull
    private final Service service;

    @NonNull
    private final OutputStream output;

    @Override
    public void run() {
        LOG.debug("Start task for: {}", service.getName());
        try {
            JsonNode response = getResponse();
            String entry = new ObjectMapper().writerFor(JsonNode.class).writeValueAsString(response);
            OutputStreamWriter writer = new OutputStreamWriter(output);

            synchronized (output) {
                writer.write(entry);
                writer.write('\n');
                writer.flush();
            }
        } catch (final Exception e) {
            LOG.error("Error with service: {}\nError message: {}", service.getName(), e.getMessage());
        }
        LOG.debug("End task for: {}", service.getName());
    }

    private JsonNode getResponse() throws IOException {
        LOG.debug("Getting response");
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String response = httpClient.execute(new HttpGet(service.getUrl()), new BasicHttpClientResponseHandler());
            LOG.debug("Got response");
            return new ObjectMapper().readTree(response);
        }
    }

}
