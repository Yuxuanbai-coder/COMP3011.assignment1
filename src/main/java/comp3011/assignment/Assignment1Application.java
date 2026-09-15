package comp3011.assignment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the Assignment 1 Spring Boot application.
 *
 * <p>The {@code @SpringBootApplication} annotation enables component scanning,
 * Spring Boot auto-configuration, and the application configuration used by
 * the controllers and services in this project. Running this class starts the
 * embedded web server on the default port, 8080.</p>
 */
@SpringBootApplication
public class Assignment1Application {

	/**
	 * Application entry point used by Eclipse, Maven, and the executable JAR.
	 *
	 * @param args optional command-line arguments passed to Spring Boot
	 */
	public static void main(String[] args) {
		SpringApplication.run(Assignment1Application.class, args);
	}

}
