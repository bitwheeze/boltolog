package bitwheeze.boltolog;

import bitwheeze.golos.goloslib.*;
import bitwheeze.golos.goloslib.model.Block;
import bitwheeze.golos.goloslib.model.Content;
import bitwheeze.golos.goloslib.model.op.Comment;
import bitwheeze.golos.goloslib.model.op.Donate;
import bitwheeze.golos.goloslib.model.op.OperationPack;
import bitwheeze.golos.goloslib.model.op.Vote;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.PostConstruct;
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
        if(content.getBody().length() > 100) {
            googleService.getCommentSentiment(content).ifPresent(sentiment -> {
                if(sentiment.getScore() > 0) {
                    chatGptService.generateComment(content, sentiment.getScore()).ifPresent(message -> postComment(post, message));
                }
            });
        } else {
            log.info("post is to short {} - {}", content.getBody().length(), post);
        }
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

            var vote = new Vote();
            vote.setVoter(config.voter);
            vote.setAuthor(post.getAuthor());
            vote.setPermlink(post.getPermlink());
            vote.setWeight(10000);

            var tr = factory.getBuidler()
                    .add(comment)
                    .add(vote)
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
