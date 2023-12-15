package bitwheeze.boltolog;

import bitwheeze.golos.goloslib.*;
import bitwheeze.golos.goloslib.model.Asset;
import bitwheeze.golos.goloslib.model.Block;
import bitwheeze.golos.goloslib.model.Content;
import bitwheeze.golos.goloslib.model.op.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
@Slf4j
@Service
public class GolosService {

    long currentBlock = 0;

    final AppConfiguration config;
    final SocialNetworkApi api;
    final DatabaseApi databaseApi;
    final NetworkBroadcastApi netApi;

    final GoogleService googleService;
    final ChatGptService chatGptService;
    final TransactionFactory factory;

    final ObjectMapper mapper;

    @PostConstruct
    void init() {
        currentBlock = config.startBlock;
    }

    @Data
    @AllArgsConstructor
    private class CommentKey {
        String author;
        String permlink;
    }

    Set<CommentKey> commented = new HashSet<>();

    @Scheduled(fixedDelay = 13000)
    public void processBlocks() {
        long headBlock = databaseApi.getDynamicGlobalProperties().block().orElseThrow().getHeadBlockNumber();
        if (headBlock <= currentBlock) {
            return;
        }
        if(currentBlock == 0) {
            currentBlock = headBlock;
            return;
        }
        for(int i = 0; i < 20 && headBlock > currentBlock; i++, currentBlock++) {
            //log.info("read block {}", currentBlock);
            var block = databaseApi.getBlock(currentBlock).block().orElseThrow();
            processBlock(block, currentBlock);
        }
    }

    public void processBlock(Block block, long blockNo) {
        if(blockNo % 100 == 0) {
            log.info("block {} with {} transactions. {}", blockNo, block.getTransactions().length, block.getTimestamp());
        }
        for(var tr : block.getTransactions()) {
            for(var opBlock : tr.getOperations()) {
                processOp(opBlock, blockNo);
            }
        }
        currentBlock = blockNo;
    }

    private void processOp(OperationPack opBlock, long blockNo) {
        if(opBlock.getOp() instanceof Comment comment) {
            processComment(comment, blockNo);
        }
    }

    private void processComment(Comment comment, long blockNo) {
        if(isBlackisted(comment)) return;
        if(isAlreadyCommented(comment)) {
            log.info("already commented {}/{}", comment.getAuthor(), comment.getPermlink());
            return;
        }
        if(isAPost(comment)) {
            makeAComment(comment);
        }

        markAsCommented(comment);
    }

    private boolean isBlackisted(Comment comment) {
        if(config.blacklist.contains(comment.getAuthor())) {
            log.info("Author {} blackisted", comment.getAuthor());
            return true;
        }
        return false;
    }

    private boolean isAPost(Comment comment) {
        return comment.getParentAuthor() == null || comment.getParentAuthor().isEmpty();
    }

    private void makeAComment(Comment post) {
        log.info("make a comment for {}/{}", post.getAuthor(), post.getPermlink());

        var content = getComment(post.getAuthor(), post.getPermlink());
        //var content = golosService.getComment("iren007", "s-etimi");

        var body = googleService.cleanupText(content.getBody());

        if(body.length() > 100) {
            googleService.getCommentSentiment(body).ifPresent(sentiment -> {
                if(sentiment.getScore() > 0) {
                    chatGptService.generateComment(content, sentiment.getScore(), body).ifPresent(message -> postComment(post, message));
                }
            });
        } else {
            log.info("post is to short {} - {}", content.getBody().length(), post);
        }
    }

    @Data
    @AllArgsConstructor
    private static class BlogDonateTarget {
        String author;
        String permlink;
    }

    private void postComment(Comment post, String message) {
        try {
            var comment = new Comment();
            comment.setAuthor(config.account);
            comment.setPermlink(getPermlink(post));
            comment.setParentAuthor(post.getAuthor());
            comment.setParentPermlink(post.getPermlink());
            comment.setBody(message);
            comment.setTitle("Ответ к теме " + post.getTitle());
            comment.setJsonMetadata("{}");
            log.info("create comment {}", comment);

            var vote1 = new Vote();
            vote1.setVoter(config.voter);
            vote1.setAuthor(post.getAuthor());
            vote1.setPermlink(post.getPermlink());
            vote1.setWeight(10000);

            var vote2 = new Vote();
            vote2.setVoter(config.account);
            vote2.setAuthor(post.getAuthor());
            vote2.setPermlink(post.getPermlink());
            vote2.setWeight(10000);

            var target = new BlogDonateTarget(post.getAuthor(), post.getPermlink());

            var memo = new DonateMemo("golos-blog", 1, mapper.writeValueAsString(target), message);
            var donate1 = new Donate(config.voter, config.account, new Asset(BigDecimal.valueOf(51).setScale(3, RoundingMode.DOWN), "GOLOS"), memo, new String[0]);
            var donate2 = new Donate(config.account, post.getAuthor(), new Asset(BigDecimal.valueOf(50).setScale(3, RoundingMode.DOWN), "GOLOS"), memo, new String[0]);

            var tr = factory.getBuidler()
                    .add(comment)
                    .add(vote1)
                    .add(vote2)
                    .add(donate1)
                    .add(donate2)
                    .buildAndSign(new String[]{config.key, config.voterKey});

            netApi.broadcastTransaction(tr).block().orElseThrow();
        } catch(Exception ex) {
            log.error("Exception sending transaction", ex);
        }
    }

    @NotNull
    private String getPermlink(Comment post) {
        return "boltolog-" + getHash(post);
    }

    private String getHash(Comment post) {
        String string = post.getAuthor() + ":" + post.getPermlink();
        return DigestUtils.md5DigestAsHex(string.getBytes(StandardCharsets.UTF_8));
    }

    private void markAsCommented(Comment comment) {
        commented.add(new CommentKey(comment.getAuthor(), comment.getPermlink()));
    }

    private boolean isAlreadyCommented(Comment comment) {
        var content = getComment(config.account, getPermlink(comment));
        return content.getAuthor() != null && !content.getAuthor().isBlank();
    }

    public  Content getComment(String author, String permlink) {
        return api.getContent(author, permlink, 0, 0).block().orElseThrow();
    }
}
