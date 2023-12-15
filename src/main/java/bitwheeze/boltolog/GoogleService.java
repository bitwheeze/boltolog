package bitwheeze.boltolog;

import bitwheeze.golos.goloslib.model.Content;
import com.google.cloud.language.v2.Document;
import com.google.cloud.language.v2.LanguageServiceClient;
import com.google.cloud.language.v2.Sentiment;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
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
    public Optional<Sentiment> getCommentSentiment(String text) {
        try {
            Document doc = Document
                    .newBuilder()
                    .setContent(text)
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

    public String cleanupText(String body) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        // You can re-use parser and renderer instances
        Node document = parser.parse(body);
        String html = renderer.render(document);  // "<p>This is <em>Sparta</em></p>\n"
        return Jsoup.parse(html).text();
    }
}
