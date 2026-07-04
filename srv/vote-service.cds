using { lunchvote as db } from '../db/schema';

/**
 * ランチ投票サービス
 *   /odata/v4/vote/Polls など
 */
service VoteService @(path: '/vote') {

  entity Polls as projection on db.Polls actions {
    /** 投票を締め切る */
    action close();
  };

  entity Options as projection on db.Options actions {
    /** この選択肢に1票入れる（重複・締切チェックあり） */
    action vote() returns Options;
  };

  @readonly
  entity Votes as projection on db.Votes;
}
