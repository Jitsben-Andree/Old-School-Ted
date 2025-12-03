package com.example.OldSchoolTeed;

import io.sentry.Sentry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OldSchoolTeedApplication {

	public static void main(String[] args) {
		SpringApplication.run(OldSchoolTeedApplication.class, args);
	}

	@Value("${sentry.dsn}")
	private String sentryDsn;

	
	@PostConstruct
	public void initSentry() {
		if (!Sentry.isEnabled()) {
			System.out.println("Sentry no auto-inició. Forzando inicialización manual...");
			try {
				Sentry.init(options -> {
					options.setDsn(sentryDsn);
					options.setTracesSampleRate(1.0);
				});
				System.out.println("Sentry inicializado manualmente con éxito.");
			} catch (Exception e) {
				System.err.println("Falló la inicialización de Sentry: " + e.getMessage());
			}
		} else {
			System.out.println("Sentry se inició automáticamente.");
		}
	}
}