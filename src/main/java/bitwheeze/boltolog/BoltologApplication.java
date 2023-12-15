package bitwheeze.boltolog;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@RequiredArgsConstructor
@SpringBootApplication
@Slf4j
public class BoltologApplication {

    private final GolosService golosService;
    private final GoogleService googleService;
    private final ChatGptService chatGptService;

    public static void main(String[] args) {
        SpringApplication.run(BoltologApplication.class, args);
    }

    @Bean
    public ApplicationRunner test() {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) throws Exception {
                //var content = golosService.getComment("bitwheeze", "ideya-v-podarok-ochen-khoroshaya-bezvozdmezdnaya");
               //var content = golosService.getComment("lex", "paru-slov-o-vcherashnem-obnovlenii-nod-blokcheina");
                //var content = golosService.getComment("ms-boss", "krupneishii-v-mire-eksperimentalnyi-termoyadernyi-reaktor-yaponii");
                var content = golosService.getComment("shadenataly", "gulyainekhochu-zimnee-nastronie");
                //var content = golosService.getComment("iren007", "s-etimi");
                var body = content.getBody();

                log.info("content {}", body);

                body = Jsoup.parse(body).text();

                log.info("content {}", body);

                MutableDataSet options = new MutableDataSet();
                Parser parser = Parser.builder(options).build();
                HtmlRenderer renderer = HtmlRenderer.builder(options).build();

                // You can re-use parser and renderer instances
                Node document = parser.parse(body);
                String html = renderer.render(document);  // "<p>This is <em>Sparta</em></p>\n"
                log.info("html {}", html);
                body = Jsoup.parse(html).text();

                log.info("content {}", body);

                final var text = body;

                if(body.length() > 100) {
                    googleService.getCommentSentiment(body).ifPresent(sentiment -> {
                        if(sentiment.getScore() > 0) {
                            chatGptService.generateComment(content, sentiment.getScore(), text).ifPresent(log::info);
                        }
                    });

                }
            }
        };
    }

}
