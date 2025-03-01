package com.kravchenko.scrapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kravchenko.scrapper.services.Service;
import com.kravchenko.scrapper.services.ServiceTask;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private interface OptionNames {

        String N_THREADS = "nThreads";
        String POLL_TIMEOUT = "timeout";
        String SERVICES = "services";
        String SAVE_FORMAT = "format";
        String HELP = "help";
    }

    public static void main(String[] args) {

        Options options = getCommandLineOptions();

        int nThreads = 0;
        long pollTimeout = 0;
        List< Service > pollServices = null;

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption(OptionNames.HELP)) {
                new HelpFormatter().printHelp("Rest Api Scrapper", options);
                System.exit(0);
            }

            if (!cmd.hasOption(OptionNames.N_THREADS)) {
                throw new MissingOptionException("Missing number of threads");
            }
            nThreads = Integer.parseInt(cmd.getOptionValue(OptionNames.N_THREADS));
            if (nThreads <= 0) {
                throw new IllegalArgumentException("Number of threads must be positive");
            }

            if (!cmd.hasOption(OptionNames.POLL_TIMEOUT)) {
                throw new MissingOptionException("Missing timeout");
            }
            pollTimeout = Long.parseLong(cmd.getOptionValue(OptionNames.POLL_TIMEOUT));
            if (pollTimeout <= 0) {
                throw new IllegalArgumentException("Timeout must be positive");
            }

            Map< String, Service > availableServices = getAvailableServices();
            if (availableServices.isEmpty()) {
                throw new IOException("No available services");
            }
            if (!cmd.hasOption(OptionNames.SERVICES)) {
                throw new MissingOptionException("Missing services");
            }
            pollServices = Arrays.stream(cmd.getOptionValues(OptionNames.SERVICES))
                .map(name -> {
                    Service service = availableServices.get(name);
                    if (service == null) {
                        LOG.warn("Not available service: {}", name);
                    }
                    return service;
                })
                .filter(Objects::nonNull)
                .toList();

            // TODO: csv support

        } catch (MissingOptionException e) {
            LOG.error("{}. Use --help for available options", e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            LOG.error("Error while retrieving available services: {}", e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            LOG.error("Command line argument parsing error: {}", e.getMessage());
            System.exit(1);
        }

        ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(nThreads);
        try (FileOutputStream output = new FileOutputStream("output.json")) {
            System.out.println("Enter any to stop");
            for (var service : pollServices) {
                scheduledExecutor.scheduleWithFixedDelay(new ServiceTask(service, output),
                    0, pollTimeout, TimeUnit.SECONDS);
            }
            new Scanner(System.in).next();
        } catch (Exception e) {
            LOG.error("Critical error: {}", e.getMessage());
        } finally {
            scheduledExecutor.shutdown();
        }
    }

    private static Options getCommandLineOptions() {
        Options options = new Options();

        Option nThreads = Option.builder("n")
            .longOpt(OptionNames.N_THREADS)
            .desc("Number of allowed simultaneously active threads")
            .hasArg()
            .argName("number")
            .build();

        Option pollTimeout = Option.builder("t")
            .longOpt(OptionNames.POLL_TIMEOUT)
            .desc("Timeout in seconds, specifying the break between service polls")
            .hasArg()
            .argName("seconds")
            .build();

        Option services = Option.builder("s")
            .longOpt(OptionNames.SERVICES)
            .desc("List of service names to be polled")
            .hasArgs()
            .valueSeparator(',')
            .numberOfArgs(Option.UNLIMITED_VALUES)
            .argName("name")
            .build();

        Option saveFormat = Option.builder("f")
            .longOpt(OptionNames.SAVE_FORMAT)
            .desc("File format for saving results")
            .hasArg()
            .argName("json|csv")
            .build();

        Option help = Option.builder("h")
            .longOpt(OptionNames.HELP)
            .desc("Start with help displayed")
            .build();

        options.addOption(nThreads);
        options.addOption(pollTimeout);
        options.addOption(services);
        options.addOption(saveFormat);
        options.addOption(help);

        return options;
    }

    private static Map< String, Service > getAvailableServices() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        URL servicesURL = Thread.currentThread().getContextClassLoader().getResource("services.json");
        List< Service > services = mapper.readValue(servicesURL, new TypeReference<>() {
        });
        return services.stream().collect(Collectors.toMap(Service::getName, Function.identity()));
    }
}
