package comp3011.assignment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test for loading the complete Spring application context.
 *
 * <p>This test checks that component scanning, configuration properties, and
 * dependency injection are wired consistently when the application starts.</p>
 */
@SpringBootTest
class Assignment1ApplicationTests {

	/**
	 * Verifies that Spring can create the application context.
	 */
	@Test
	void contextLoads() {
	}

}
