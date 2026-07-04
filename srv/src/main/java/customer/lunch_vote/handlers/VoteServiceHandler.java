package customer.lunch_vote.handlers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.voteservice.Options;
import cds.gen.voteservice.OptionsVoteContext;
import cds.gen.voteservice.Options_;
import cds.gen.voteservice.Polls;
import cds.gen.voteservice.PollsCloseContext;
import cds.gen.voteservice.Polls_;
import cds.gen.voteservice.VoteService_;
import cds.gen.voteservice.Votes;
import cds.gen.voteservice.Votes_;

/**
 * ランチ投票サービスの業務ロジック。
 *   - vote()  : 締切・重複チェックのうえ1票を記録し、得票数を加算
 *   - close() : 投票を締め切る
 *   - READ    : Polls 読み取り時に総投票数を集計
 */
@Component
@ServiceName(VoteService_.CDS_NAME)
public class VoteServiceHandler implements EventHandler {

  private final PersistenceService db;

  public VoteServiceHandler(PersistenceService db) {
    this.db = db;
  }

  /** Options.vote() — この選択肢に1票入れる */
  @On(entity = Options_.CDS_NAME)
  public void onVote(OptionsVoteContext context) {
    // アクション対象の選択肢を取得
    Options option = db.run(context.getCqn()).single(Options.class);
    String pollId = option.getPollId();

    String user = context.getUserInfo().getName();
    final String voter = (user == null || user.isEmpty()) ? "anonymous" : user;

    // 投票の状態チェック（クローズ・締切）
    Polls poll = db.run(Select.from(Polls_.class).where(p -> p.ID().eq(pollId)))
        .single(Polls.class);
    if (Boolean.TRUE.equals(poll.getClosed())) {
      throw new ServiceException(ErrorStatuses.CONFLICT, "この投票はすでに締め切られています");
    }
    if (poll.getDeadline() != null && Instant.now().isAfter(poll.getDeadline())) {
      throw new ServiceException(ErrorStatuses.CONFLICT, "締切時刻を過ぎているため投票できません");
    }

    // 重複投票チェック（1ユーザーにつき1票）
    long already = db.run(Select.from(Votes_.class)
        .where(v -> v.poll_ID().eq(pollId).and(v.voter().eq(voter)))).rowCount();
    if (already > 0) {
      throw new ServiceException(ErrorStatuses.CONFLICT, "すでにこの投票で1票を投じています");
    }

    // 投票を記録
    Votes vote = Votes.create();
    vote.setId(UUID.randomUUID().toString());
    vote.setPollId(pollId);
    vote.setOptionId(option.getId());
    vote.setVoter(voter);
    db.run(Insert.into(Votes_.class).entry(vote));

    // 得票数を +1
    int newCount = (option.getVoteCount() == null ? 0 : option.getVoteCount()) + 1;
    option.setVoteCount(newCount);
    db.run(Update.entity(Options_.class).entry(option));

    context.setResult(option);
    context.setCompleted();
  }

  /** Polls.close() — 投票を締め切る */
  @On(entity = Polls_.CDS_NAME)
  public void onClose(PollsCloseContext context) {
    Polls poll = db.run(context.getCqn()).single(Polls.class);
    poll.setClosed(true);
    db.run(Update.entity(Polls_.class).entry(poll));
    context.setCompleted();
  }

  /** Polls 読み取り時に総投票数を集計して virtual フィールドに設定 */
  @After(event = CqnService.EVENT_READ, entity = Polls_.CDS_NAME)
  public void computeTotals(List<Polls> polls) {
    for (Polls p : polls) {
      if (p.getId() == null) {
        continue;
      }
      long total = db.run(Select.from(Votes_.class)
          .where(v -> v.poll_ID().eq(p.getId()))).rowCount();
      p.setTotalVotes((int) total);
    }
  }
}
