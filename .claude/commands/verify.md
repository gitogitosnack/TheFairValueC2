---
description: ビルド・JS テスト・IDE 診断をまとめて実行して結果を報告する
allowed-tools: Bash, Read, Glob, Grep, mcp__ide__getDiagnostics
---

現在の変更内容を検証してください。

1. `mvn clean compile` を実行する（Doma のアノテーションプロセッサを通すため `clean` は省略しない）
2. `npx vitest run` を実行する
3. `mcp__ide__getDiagnostics` で IDE の診断を取得する
4. `git status --short` と `git diff --stat` で変更範囲を確認する

報告のしかた：

- 失敗があれば、エラーメッセージの該当部分と原因箇所を `ファイルパス:行番号` 形式で示す
- `[DOMA4019]` が出た場合は `.claude/skills/doma-dao/SKILL.md` と `docs/doma4019-vscode.md` の切り分け手順に従う
- すべて通った場合は、Java テストが存在しないため画面での動作確認が別途必要なことを添える
- 検証結果は事実のまま報告する。通っていないものを「通った」と書かない
