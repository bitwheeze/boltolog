package bitwheeze.boltolog;

import bitwheeze.golos.goloslib.model.Content;
import com.google.cloud.language.v2.Document;
import com.google.cloud.language.v2.LanguageServiceClient;
import com.google.cloud.language.v2.Sentiment;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Service
public class GoogleService {

    private final AppConfiguration config;
    private final LanguageServiceClient languageClient;

    @PostConstruct
    public void init() {
        log.info("set google key as {}", config.google.key);
        System.setProperty(
                "GOOGLE_APPLICATION_CREDENTIALS",
                config.google.key);
    }

    @SneakyThrows
    public Optional<Sentiment> getCommentSentiment(Content content) {
        try {
            Document doc = Document
                    .newBuilder()
                    .setContent(content.getBody())
                    .setType(Document.Type.PLAIN_TEXT)
                    .build();

            Sentiment sentiment = languageClient
                    .analyzeSentiment(doc)
                    .getDocumentSentiment();

            log.info("Sentiment {}, Magnitude {}", sentiment.getScore(), sentiment.getMagnitude());

            return Optional.of(sentiment);
        } catch (Exception ex) {
            log.error("Error analyzing sentiment: {}", ex.getMessage());
        }
        return Optional.empty();
    }
}
