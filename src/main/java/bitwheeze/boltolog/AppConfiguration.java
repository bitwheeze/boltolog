package bitwheeze.boltolog;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "boltolog")
@ComponentScan("bitwheeze.golos.goloslib")
@EnableScheduling
public class AppConfiguration {
    GoogleProps google;
    OpenaiProps openai;
    String account;
    String key;
    String blacklist = "opennetru,one-armed,oldpages,golos.lotto,lllll1ll,sonya44";
    long startBlock = 0;

    String voter;
    String voterKey;
}
