package bitwheeze.boltolog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    /*
    @Bean
    public ApplicationRunner test() {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) throws Exception {
                //var content = golosService.getComment("bitwheeze", "ideya-v-podarok-ochen-khoroshaya-bezvozdmezdnaya");
               //var content = golosService.getComment("lex", "paru-slov-o-vcherashnem-obnovlenii-nod-blokcheina");
                var content = golosService.getComment("lex", "re-boltolog-boltolog-c66cfda820296cdde115ac5c2772d4a9-20231211t234235848z");
                //var content = golosService.getComment("iren007", "s-etimi");
                log.info("content {}", content.getBody());
                if(content.getBody().length() > 10) {
                    googleService.getCommentSentiment(content).ifPresent(sentiment -> {
                        //if(sentiment.getScore() > 0) {
                            chatGptService.generateAnswer(content, sentiment.getScore()).ifPresent(log::info);
                        //}
                    });

                }
            }
        };
    }

     */

}
