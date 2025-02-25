package com.kravchenko.scrapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kravchenko.scrapper.configuration.OutputType;
import com.kravchenko.scrapper.services.Service;

import java.io.IOException;
import java.util.List;

public class Main {

    /**
     *
     * @param args arguments of app, list of available:
     *             -time <seconds> - poll timeout in seconds
     *             -threads <number> - number of allowed simultaneously active polling threads
     *             [-output <csv|json>] - output file extension, if not specified will be json
     */
    public static void main(String[] args) {

        final long n = 5;
        final long timeout = 10;
        final OutputType type = OutputType.JSON;

        List< Service > services = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            services = mapper.readValue(classLoader.getResource("services.json"), new TypeReference<>(){});
        } catch (IOException e) {
            System.err.println("Couldn't read services list");
            return;
        }
    }
}
