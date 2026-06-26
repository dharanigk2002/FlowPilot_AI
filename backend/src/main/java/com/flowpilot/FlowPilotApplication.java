package com.flowpilot;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class FlowPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowPilotApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(Environment environment) {
        return runner -> {
            System.out.println("Server running on port: "+ environment.getProperty("local.server.port"));
        };
    }
}
