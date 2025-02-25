package com.kravchenko.scrapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kravchenko.scrapper.configuration.OutputType;
import com.kravchenko.scrapper.services.Service;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(Main.class);
        logger.info("test message");

        final int n = 5;
        final long timeout = 10;
        final OutputType type = OutputType.JSON;

        List< Service > services;
        ObjectMapper mapper = new ObjectMapper();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            services = mapper.readValue(classLoader.getResource("services.json"), new TypeReference<>() {
            });
        } catch (IOException e) {
            System.err.println("Couldn't read services list");
            return;
        }

        ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(n);
        String filename = "output." + switch (type) {
            case CSV -> "csv";
            default -> "json";
        };
        File output = new File(filename);
        for (var service : services) {
            scheduledExecutor.scheduleWithFixedDelay(() -> {
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    String entityBody = httpClient.execute(new HttpGet(service.getUrl()), response -> {
                        if (response.getCode() != 200) {
                            throw new IOException("Bad response code: " + response.getCode());
                        }
                        HttpEntity entity = response.getEntity();
                        return EntityUtils.toString(entity);
                    });
                    System.out.println(entityBody);
                } catch (IOException e) {
                    System.err.println("Error with service: " + service.getName());
                    System.err.println(e.getMessage());
                }

            }, 0, timeout, TimeUnit.SECONDS);
        }
        new Scanner(System.in).nextInt();
        scheduledExecutor.shutdown();
    }
}
