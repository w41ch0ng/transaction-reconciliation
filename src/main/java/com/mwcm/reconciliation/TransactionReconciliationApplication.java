package com.mwcm.reconciliation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TransactionReconciliationApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransactionReconciliationApplication.class, args);
	}

}
