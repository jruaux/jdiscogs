package org.ruaux.jdiscogs;

import org.ruaux.jdiscogs.data.JDiscogsBatchConfiguration;
import org.ruaux.jdiscogs.data.JDiscogsBatchConfiguration.LoadJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JDiscogsApplication implements ApplicationRunner {

	@Autowired
	private JDiscogsBatchConfiguration batch;

	public static void main(String[] args) {
		SpringApplication.run(JDiscogsApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		batch.run(LoadJob.Masters, LoadJob.Releases);
	}

}
