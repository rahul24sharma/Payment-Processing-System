package com.payment.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "spring.main.lazy-initialization=true",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false"
})
@ActiveProfiles("test")
class NotificationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
