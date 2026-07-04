using VoteService from '../srv/vote-service';

//
// Polls 一覧 / 詳細ページ
//
annotate VoteService.Polls with @(
  UI.HeaderInfo: {
    TypeName      : 'ランチ投票',
    TypeNamePlural: 'ランチ投票',
    Title         : { Value: title },
    Description   : { Value: description }
  },
  UI.LineItem: [
    { Value: title,      Label: 'タイトル' },
    { Value: deadline,   Label: '締切' },
    { Value: totalVotes, Label: '総投票数' },
    { Value: closed,     Label: '締切済', Criticality: closed }
  ],
  UI.FieldGroup #Main: {
    Data: [
      { Value: title,       Label: 'タイトル' },
      { Value: description,  Label: '説明' },
      { Value: deadline,     Label: '締切' },
      { Value: closed,       Label: '締切済' },
      { Value: totalVotes,   Label: '総投票数' }
    ]
  },
  UI.Facets: [
    { $Type: 'UI.ReferenceFacet', Label: '概要',   Target: '@UI.FieldGroup#Main' },
    { $Type: 'UI.ReferenceFacet', Label: '選択肢', Target: 'options/@UI.LineItem' }
  ]
);

//
// Options（選択肢）— 詳細ページ内のテーブル + 投票ボタン
//
annotate VoteService.Options with @(
  UI.LineItem: [
    { Value: emoji,     Label: '' },
    { Value: name,      Label: 'お店 / メニュー' },
    { Value: voteCount, Label: '得票数' },
    {
      $Type : 'UI.DataFieldForAction',
      Action: 'VoteService.vote',
      Label : '👍 これに投票'
    }
  ]
);
