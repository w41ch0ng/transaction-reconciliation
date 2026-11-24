package com.mwcm.reconciliation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
class TransactionReconciliationApplicationTests {

	@Test
	void contextLoads() {
	}

}
