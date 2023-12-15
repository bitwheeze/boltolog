package bitwheeze.boltolog;

import bitwheeze.golos.goloslib.model.Content;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatGptService {

    private final OpenAiService openAiService;

    public Optional<String> generateComment(Content content, double temperature, String body) {
        try {
            if(body.length() < 200) return Optional.empty();
            if(body.length() > 6500) body = body.substring(0, 6500);
            var request = ChatCompletionRequest.builder().messages(List.of(
                            new ChatMessage("system", "you are young community participant, be informal"),
                            new ChatMessage("user", "Please write a short positive comment for following text using 20 tokens or less in russian!"),
                            new ChatMessage("user", body)))
                    .maxTokens(100)
                    //.model("gpt-3.5-turbo")
                    .model("gpt-4-1106-preview")
                    .temperature(0.2)
                    .build();

            var resonse = openAiService.createChatCompletion(request);
            if (!resonse.getChoices().isEmpty()) {
                return Optional.of(resonse.getChoices().get(0).getMessage().getContent());
            }
        } catch(Exception ex) {
            log.error("Error generating comment", ex);
        }
        return Optional.empty();
    }

    public Optional<String> generateAnswer(Content content, double temperature) {
        try {
            var body = content.getBody();
            if(body.length() < 20) return Optional.empty();
            if(body.length() > 6500) body = body.substring(0, 6500);
            var request = ChatCompletionRequest.builder().messages(List.of(
                            new ChatMessage("system", "you are young community participant, be informal"),
                            new ChatMessage("user", "Please write a short positive comment for following text using 20 tokens or less in russian!"),
                            new ChatMessage("user", body)))
                    .maxTokens(100)
                    //.model("gpt-3.5-turbo")
                    .model("gpt-4")
                    .temperature(0.1)
                    .build();

            var resonse = openAiService.createChatCompletion(request);
            if (!resonse.getChoices().isEmpty()) {
                return Optional.of(resonse.getChoices().get(0).getMessage().getContent());
            }
        } catch(Exception ex) {
            log.error("Error generating comment", ex);
        }
        return Optional.empty();
    }

}
