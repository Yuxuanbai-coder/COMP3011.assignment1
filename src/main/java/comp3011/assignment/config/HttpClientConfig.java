package comp3011.assignment.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configures the HTTP client used for calls to the external transcription API.
 *
 * <p>The client uses the JDK HTTP implementation so it can be used by
 * Spring's {@link RestClient}. Explicit connection and read timeouts prevent
 * an unavailable Cloud service from holding a request indefinitely.</p>
 */
@Configuration
public class HttpClientConfig {

    /**
     * Creates the RestClient used by {@code TranscriptionService}.
     *
     * @param restClientBuilder Spring's RestClient builder
     * @return a RestClient with bounded connection and response wait times
     */
    @Bean
    public RestClient transcriptionRestClient(RestClient.Builder restClientBuilder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }
}
