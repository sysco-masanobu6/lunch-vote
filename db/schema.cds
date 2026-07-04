namespace lunchvote;

using { cuid, managed } from '@sap/cds/common';

/**
 * 1回のランチ投票（例：「今日のランチどこ行く？」）
 */
entity Polls : cuid, managed {
  title       : String(100) not null;
  description : String(500);
  deadline    : DateTime;                 // 締切。過ぎたら投票不可
  closed      : Boolean default false;    // 手動クローズ用
  options     : Composition of many Options on options.poll = $self;
  virtual totalVotes : Integer;           // 集計（ハンドラで算出）
}

/**
 * 投票の選択肢（お店やメニュー）
 */
entity Options : cuid {
  poll      : Association to Polls;
  name      : String(100) not null;       // 店名 / メニュー名
  emoji     : String(8);                  // 🍜🍛🍣 など
  voteCount : Integer default 0;          // 集計値（票が入るたびに加算）
  votes     : Composition of many Votes on votes.option = $self;
}

/**
 * 1票。1ユーザーは1つのPollにつき1票まで（ハンドラで制御）
 */
entity Votes : cuid, managed {
  poll   : Association to Polls;
  option : Association to Options;
  voter  : String(255);                   // 投票者（ログインユーザー）
}
