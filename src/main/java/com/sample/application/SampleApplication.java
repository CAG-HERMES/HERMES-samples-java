package com.sample.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Enable scheduling in Spring Boot
public class SampleApplication {
	@Autowired
	private SolaceSubscriberService solaceSubscriberService;


	public static void main(String[] args) {
		try {
			SpringApplication.run(com.sample.application.SampleApplication.class, args);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Bean
	public CommandLineRunner run() {
		return args -> {
			solaceSubscriberService.connect();
			solaceSubscriberService.startScheduler();
			solaceSubscriberService.startSubcribeTopic();
			solaceSubscriberService.startReceivingFromQueue();

			// Add shutdown hook
			Runtime.getRuntime().addShutdownHook(new Thread(solaceSubscriberService::stopSubcribeTopic));
			Runtime.getRuntime().addShutdownHook(new Thread(solaceSubscriberService::stopReceivingFromQueue));

		};
	}

}
