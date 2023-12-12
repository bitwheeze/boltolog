package bitwheeze.boltolog;

import com.google.api.gax.core.FixedCredentialsProvider;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.language.v2.LanguageServiceClient;
import com.google.cloud.language.v2.LanguageServiceSettings;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.time.Duration;
import java.util.Arrays;

@RequiredArgsConstructor
@Configuration
public class GoogleConfiguration {
    private final AppConfiguration config;

    @Bean
    @SneakyThrows
    public LanguageServiceClient languageClient() {
        var credentials = FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(new FileInputStream(config.google.key)));

        var settings = LanguageServiceSettings.newBuilder().setCredentialsProvider(credentials).build();
        return LanguageServiceClient.create(settings);
    }

    @Bean
    public OpenAiService openAiService() {
        return new OpenAiService(config.openai.key, Duration.ofSeconds(20L));
    }

}
