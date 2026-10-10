# ログイン画面・認証機能 設計書（ドラフト）

- 作成日: 2026-10-03
- 状態: **設計レビュー用ドラフト。コード未着手。**
- 関連文書: [問題・課題リスト](login-issues.md)（本文中の `L-xx` は同リストの ID）

`src/main/resources/templates/login/chasing_cats.html`（猫が追いかけてくるログイン画面のモック）を、
TheFairValue の正式なログイン画面として組み込むための設計である。
現状のアプリには認証の仕組みが一切ないため、画面の組み込みだけでなく
「認証基盤の新設」「既存画面・既存 Ajax への影響対応」までを範囲に含める。

---

## 1. 目的とスコープ

### 1.1 目的

- 未ログインの利用者が銘柄一覧・銘柄詳細・マスタ・設定・バッチ手動実行へアクセスできないようにする
- `chasing_cats.html` の見た目とアニメーションを保ったまま、実際にログインできる画面にする
- 全画面のヘッダーにある「テスト太郎 様」「ログアウト」を実データ・実動作に置き換える

### 1.2 第 1 フェーズの範囲（本設計書の対象）

| # | 内容 |
|---|---|
| 1 | メールアドレス＋パスワードによるログイン（セッション方式） |
| 2 | ログアウト |
| 3 | 未ログイン時のアクセス制御（画面はログイン画面へ、REST は 401） |
| 4 | CSRF 対策と、それに伴う既存 Ajax / fetch の改修 |
| 5 | ヘッダーのユーザ名表示・ログアウトボタンの実装 |
| 6 | 初期ユーザの登録手段 |

### 1.3 範囲外（課題リストで管理）

| 内容 | 課題 ID |
|---|---|
| 新規登録（サインアップ）画面 | L-10 |
| パスワード再設定（メール送信） | L-11 |
| 設定画面のアカウント情報・パスワード変更の実装 | L-12 |
| ロール（管理者／一般）による権限分け | L-13 |
| ログイン失敗回数によるアカウントロック | L-14 |
| ログイン状態の保持（Remember-me） | L-15 |
| ユーザごとのデータ分離（シミュレーション・パラメータ） | L-16 |

---

## 2. 現状調査の結果

### 2.1 すでにあるもの

| 対象 | 状態 |
|---|---|
| `users` テーブル | 存在する。`id` / `username`(50, UNIQUE) / `email`(255, UNIQUE) / `password_hash`(255) / `preferred_currency_id` |
| `UserEntity` / `UserDao` | 存在する。`selectAll` / `selectById` / insert / update / delete のみ。**どこからも呼ばれていない** |
| `chasing_cats.html` | 静的なモック。CSS と JS はすべてインライン |
| 設定画面のログアウトボタン | `/top` へ遷移するだけのモック（`settings.js:146-151`） |

### 2.2 足りないもの

| 対象 | 状態 |
|---|---|
| Spring Security | `pom.xml` に依存なし。全 URL が無認証で公開されている |
| ログイン画面を返す Controller | なし。`chasing_cats.html` はどの URL からも表示されない |
| メールアドレスでユーザを引く DAO メソッド | なし |
| パスワードのハッシュ化処理 | なし |
| CSRF トークンの送信 | 既存 Ajax / fetch 16 箇所（JS 7 ファイル）すべて未対応 |
| 共通ヘッダー | なし。ナビゲーションは 6 テンプレートにコピーされ、ユーザ名は「テスト太郎 様」固定 |
| ルート URL `/` のマッピング | なし |
| `users` の DDL | `db/migration`（V001〜V005）に含まれていない |

### 2.3 `chasing_cats.html` の現状

| 箇所 | 現状 | 対応 |
|---|---|---|
| `:2` | `xmlns:th` がない | Thymeleaf 名前空間を追加 |
| `:6` | タイトルが「可愛い猫が寄り添う ログイン画面」 | 「ログイン - TheFairValue」に変更 |
| `:7-136` | CSS がインライン | `static/css/login.css` へ分離 |
| `:146` | `<form onsubmit="event.preventDefault()">` で送信を止めている | `th:action="@{/login}" method="post"` に変更 |
| `:149-164` | `input` に `name` 属性がない | `name="email"` / `name="password"` を追加 |
| — | エラーメッセージ表示欄がない | カード内に追加 |
| `:171` | 「新規登録」リンクが `href="#"` | 第 1 フェーズでは非表示（L-10） |
| `:176-481` | 猫アニメーションの JS がインライン | `static/js/login.js` へ分離 |
| `:194` | `mousemove` のみ対応 | タッチ端末では猫が自律行動のみになる（L-20） |

---

## 3. 方式設計

### 3.1 採用方式

| 項目 | 採用案 | 理由 |
|---|---|---|
| 認証フレームワーク | Spring Security 6（Spring Boot 3.2.4 の管理バージョン） | 自前実装よりも安全。セッション固定化対策・CSRF 対策が標準で付く |
| 認証方式 | フォームログイン＋サーバセッション | Thymeleaf の画面遷移型アプリであり、JWT にする理由がない |
| ログイン ID | メールアドレス | 画面の入力項目がメールアドレスであるため |
| パスワード保管 | BCrypt（`BCryptPasswordEncoder`） | ハッシュは 60 文字。`password_hash varchar(255)` に収まる |
| CSRF 対策 | 有効（Spring Security 既定） | 画面はセッション Cookie で認証するため必須 |
| 画面でのユーザ情報参照 | `thymeleaf-extras-springsecurity6` | `sec:authentication` でヘッダーにユーザ名を出せる |

第 1 フェーズでは **`users` テーブルの定義変更は不要**である（既存カラムで足りる）。

### 3.2 追加する依存（`pom.xml`）

| artifactId | scope | 用途 |
|---|---|---|
| `spring-boot-starter-security` | compile | 認証・認可・CSRF |
| `thymeleaf-extras-springsecurity6` | compile | テンプレートからの認証情報参照 |
| `spring-security-test` | test | `@WithMockUser`、`csrf()` |

いずれも Spring Boot の BOM でバージョン管理されるため、バージョン指定は書かない。

### 3.3 パッケージ構成

認証は銘柄機能ではないため、`web.stock` の下ではなく `web.auth` を新設する（L-17 で要確認）。
`dao` と `entity` は既存ルールどおり `web.dao` / `web.entity` に置く。

```
src/main/java/org/example/web/
├── auth/
│   ├── config/SecurityConfig.java            ... SecurityFilterChain, PasswordEncoder
│   ├── controller/LoginController.java       ... GET /login
│   ├── controller/RootController.java        ... GET /  → /top へリダイレクト
│   ├── domain/LoginUser.java                 ... UserDetails 実装（id, 表示名, email）
│   ├── service/LoginUserDetailsService.java  ... UserDetailsService を継承するインタフェース
│   ├── service/LoginUserDetailsServiceImpl.java
│   └── service/InitialUserRegistrar.java     ... 初期ユーザ登録（5.4）
├── dao/UserDao.java                          ... selectByEmail を追加
└── entity/UserEntity.java                    ... 変更なし

src/main/resources/
├── META-INF/org/example/web/dao/UserDao/selectByEmail.sql
├── templates/login/chasing_cats.html         ... 改修
├── templates/fragments/header.html           ... 新設（共通 head・ナビゲーション）
├── static/css/login.css                      ... 新設（インライン CSS を移動）
├── static/js/login.js                        ... 新設（猫アニメーションを移動）
└── static/js/csrf.js                         ... 新設（Ajax / fetch 共通の CSRF・401 処理）
```

Service は既存規約どおりインタフェース＋`Impl` の 2 ファイル、DI はコンストラクタインジェクションとする。

---

## 4. URL 設計

### 4.1 追加する URL

| メソッド | URL | 処理 | 認証 |
|---|---|---|---|
| GET | `/login` | ログイン画面を表示 | 不要 |
| POST | `/login` | 認証処理（Spring Security が処理。Controller は書かない） | 不要 |
| POST | `/logout` | ログアウト（Spring Security が処理） | 必要 |
| GET | `/` | `/top` へリダイレクト | 必要 |

### 4.2 アクセス制御

| URL パターン | 制御 |
|---|---|
| `/login`、`/css/**`、`/js/**`、`/error` | 全員許可 |
| 上記以外すべて | ログイン済みのみ |

### 4.3 未ログイン時の応答

画面と REST で応答を分ける。REST まで 302 でログイン画面へ飛ばすと、Ajax 側がログイン画面の HTML を
成功レスポンスとして受け取ってしまうためである。

| リクエスト先 | 応答 |
|---|---|
| 画面（`/top`、`/countries` など） | 302 で `/login` へ。ログイン後は元の URL へ戻す |
| REST（`/rest_**`） | 401。JS 側の共通処理が `/login` へ遷移させる |

`SecurityConfig` では `/rest_**` に一致するリクエストへ `HttpStatusEntryPoint(401)` を割り当てる
（`exceptionHandling().defaultAuthenticationEntryPointFor(...)`）。

### 4.4 画面遷移

```
未ログインで /top へアクセス ─→ /login ─(成功)→ /top（または元の URL）
                                   │
                                   └(失敗)→ /login?error

ログアウトボタン ─→ POST /logout ─→ /login?logout

ログイン済みで /login へアクセス ─→ /top へリダイレクト
```

---

## 5. 処理設計

### 5.1 ログイン

1. 利用者がメールアドレスとパスワードを入力し「ログイン」を押す
2. ブラウザが `POST /login` を送る（`email`、`password`、`_csrf`）
3. Spring Security が `LoginUserDetailsServiceImpl.loadUserByUsername(email)` を呼ぶ
4. Service が `UserDao.selectByEmail(email)` でユーザを取得する。入力値は前後の空白を除き小文字にそろえてから検索する（L-07）
5. 見つからなければ `UsernameNotFoundException` を投げる
6. 見つかれば `LoginUser` を返し、Spring Security が BCrypt でパスワードを照合する
7. 成功: セッション ID を再発行し、`/top`（または元の URL）へリダイレクトする
8. 失敗: `/login?error` へリダイレクトする

`SecurityConfig` の設定値:

| 設定 | 値 |
|---|---|
| `loginPage` | `/login` |
| `usernameParameter` | `email` |
| `passwordParameter` | `password` |
| `defaultSuccessUrl` | `/top` |
| `failureUrl` | `/login?error` |
| `logoutSuccessUrl` | `/login?logout` |

### 5.2 ログアウト

- Spring Security の既定どおり `POST /logout`（CSRF トークン必須）とする。GET でのログアウトは作らない
- セッション破棄と `JSESSIONID` Cookie の削除を行う
- ボタンは共通ヘッダー内の `<form th:action="@{/logout}" method="post">` に置く
- 設定画面の「ログアウトする」ボタン（`#btn-logout-settings`）も同じフォーム送信に変える。
  `settings.js:146-151` のモック処理と `settings.test.js:88-99` のテストは削除または書き換える（L-05）

### 5.3 `LoginUser`（`UserDetails` 実装）

| 項目 | 元データ | 用途 |
|---|---|---|
| `id` | `users.id` | 将来のユーザ別データ取得 |
| `displayName` | `users.username` | ヘッダー表示「〇〇 様」 |
| `getUsername()` | `users.email` | 認証上の識別子 |
| `getPassword()` | `users.password_hash` | 照合用 |
| `getAuthorities()` | 固定で `ROLE_USER` | 第 1 フェーズはロール分けなし（L-13） |

`UserEntity` をそのままセッションに入れず、必要な項目だけを `LoginUser` に写す。

### 5.4 初期ユーザの登録

サインアップ画面を作らないため、最初の 1 人を登録する手段が必要になる（L-03）。

採用案: 起動時に `users` が 0 件のときだけ、環境変数の値で 1 件登録する（`InitialUserRegistrar`、`ApplicationRunner`）。

| 環境変数 | 内容 |
|---|---|
| `APP_INITIAL_USER_EMAIL` | メールアドレス |
| `APP_INITIAL_USER_NAME` | 表示名 |
| `APP_INITIAL_USER_PASSWORD` | 平文パスワード（起動時に BCrypt 化して保存） |

- 環境変数が未設定なら何もしない
- `users` に 1 件でもあれば何もしない
- パスワードを `application.properties` や SQL ファイルに書かない

`users` に既存データがある場合の扱いは L-02 を参照。

### 5.5 DAO の追加

`UserDao` に次を追加する。

```java
@Select
Optional<UserEntity> selectByEmail(String email);
```

```sql
-- META-INF/org/example/web/dao/UserDao/selectByEmail.sql
select
  /*%expand*/*
from
  users
where
  lower(email) = /* email */'test@example.com'
```

あわせて `selectById(Long id)` の引数型を、Entity の `id`（`Integer`）に合わせて直す（L-08）。

---

## 6. 画面設計

### 6.1 ログイン画面（`templates/login/chasing_cats.html`）

見た目とアニメーションは現状のまま維持する。

| 項目 | 種別 | name | 必須 | 備考 |
|---|---|---|---|---|
| メールアドレス | `input type="email"` | `email` | ○ | `autocomplete="username"`、`autofocus` |
| パスワード | `input type="password"` | `password` | ○ | `autocomplete="current-password"` |
| ログイン | `button type="submit"` | — | — | |
| メッセージ欄 | `p` | — | — | 6.2 参照 |

- CSRF トークンの hidden 項目は、`th:action` を使うことで Thymeleaf が自動挿入する
- 「新規登録」リンクは第 1 フェーズでは出さない
- 猫を描く `<canvas>` はカードの背面（`z-index: 1`）にあり、フォーム操作を妨げない。この構造は変えない

### 6.2 メッセージ

| 条件 | 表示文言 | 色 |
|---|---|---|
| `?error` | メールアドレスまたはパスワードが正しくありません | 赤系 |
| `?logout` | ログアウトしました | 緑系 |

存在しないメールアドレスとパスワード間違いを同じ文言にする。登録済みアドレスを外部から推測させないためである。

### 6.3 CSS / JS の分離

| 移動元（`chasing_cats.html`） | 移動先 |
|---|---|
| `<style>` 内すべて | `static/css/login.css` |
| `<script>` 内すべて（`CuteCat` クラス、`animate()`） | `static/js/login.js` |

プロジェクトの「JS は `static/js/<feature>.js`」という規約に合わせるためと、
猫の状態遷移（歩く・毛づくろい・寝る）を Vitest でテストできるようにするためである。

### 6.4 共通ヘッダー（`templates/fragments/header.html`）

現在 6 テンプレートにコピーされているナビゲーションを 1 つのフラグメントにまとめる。

| フラグメント | 内容 |
|---|---|
| `csrfMeta` | `<meta name="_csrf">`、`<meta name="_csrf_header">` |
| `navbar(active)` | ロゴ、メニュー、ユーザ名、ログアウトフォーム |

- ユーザ名: `<span class="user-name" sec:authentication="principal.displayName">` ＋「様」
- ログアウト: `<form th:action="@{/logout}" method="post"><button class="btn-logout">ログアウト</button></form>`

対象テンプレート: `stock-list`、`country-list`、`currency-list`、`industry-list`、
`valuation-model-list`、`settings`。
`stock-detail002.html` はナビゲーションを持たないが、Ajax を使うため `csrfMeta` だけは必要になる（L-06）。

---

## 7. 既存機能への影響と対応

### 7.1 CSRF トークン（影響が最も大きい）

Spring Security を入れると、トークンのない POST / DELETE はすべて 403 になる。
対象は次の 7 ファイル・16 箇所の通信で、うち 14 箇所が POST / DELETE である。

| ファイル | 方式 | 箇所数 |
|---|---|---|
| `country.js` / `currency.js` / `industry.js` / `valuation-model.js` | `$.ajax` | 各 2（登録更新 POST、削除 DELETE） |
| `stock-list.js` | `$.ajax` | 2（登録更新 POST、削除 DELETE） |
| `stock-detail_002.js` | `fetch` | 5（うち GET 2、POST 2、DELETE 1） |
| `settings.js` | `fetch` | 1（POST） |

対応: 共通の `static/js/csrf.js` を新設し、各テンプレートで他の JS より先に読み込む。

| 機能 | 内容 |
|---|---|
| jQuery 向け | `$.ajaxSetup` の `beforeSend` で CSRF ヘッダーを付ける |
| fetch 向け | `window.fetch` を薄く包み、同一オリジンの GET 以外に CSRF ヘッダーを付ける |
| 401 の共通処理 | `$(document).ajaxError` と fetch ラッパーで 401 を検知し `/login` へ遷移する |

この方式なら既存 16 箇所の呼び出しコード自体は書き換えずに済む。

### 7.2 Excel 出力

`master-list-export.js:22` は `window.location.href` による GET 遷移であり、CSRF の影響を受けない。
未ログイン時は通常の画面と同じくログイン画面へ遷移する。

### 7.3 バッチ

| 経路 | 影響 |
|---|---|
| 設定画面からの手動実行（`POST /rest_market_data_sync/*`） | 認証と CSRF トークンが必要になる。7.1 の対応で動く |
| 内蔵スケジューラ（`MarketDataSyncScheduler`） | HTTP を経由しないため影響なし |
| 将来のバッチ専用 JAR（`MarketDataBatchApplication`） | 非 Web 起動のため Security のフィルタは動かない。`InitialUserRegistrar` がバッチ起動時に動かないよう条件を付ける（L-18） |

### 7.4 既存テスト

既存の Java テストに `@SpringBootTest` / `@WebMvcTest` を使うものはなく、Spring コンテキストを起動しない
（大半が `MockitoExtension` による単体テスト）。
そのため Spring Security を追加しても既存テストは影響を受けない見込みである（製造時に `mvn test` で確認する）。
JS テストは `settings.test.js` のログアウトのケースだけ修正が必要になる。

---

## 8. セッション・セキュリティ設定

| 項目 | 設定 | 備考 |
|---|---|---|
| セッションタイムアウト | 30 分（`server.servlet.session.timeout=30m`） | L-19 で要確認 |
| セッション固定化対策 | ログイン成功時にセッション ID を再発行 | Spring Security 既定 |
| Cookie `HttpOnly` | 有効 | 既定 |
| Cookie `SameSite` | `lax` | `server.servlet.session.cookie.same-site=lax` |
| Cookie `Secure` | HTTPS 環境でのみ有効化 | ローカルは HTTP のため環境変数で切り替える（L-09） |
| パスワードのログ出力 | しない | `logging.level` が DEBUG のため、フォーム値が出ないことを確認する |

セッションはメモリ保持のため、アプリ再起動（devtools の自動再起動を含む）でログイン状態は失われる。
開発中は再ログインが頻発するが、第 1 フェーズでは許容する。

---

## 9. テスト方針

| 対象 | 方法 | 確認内容 |
|---|---|---|
| `LoginUserDetailsServiceImpl` | JUnit＋Mockito | 該当あり／なし、メールアドレスの正規化 |
| `SecurityConfig` | `@WebMvcTest`＋`spring-security-test` | 未ログインの画面は 302、REST は 401、トークンなし POST は 403、ログイン成功・失敗の遷移先 |
| `LoginController` | 既存 Controller テストと同じ形式 | ビュー名、ログイン済み時のリダイレクト |
| `login.js` | Vitest | 猫の状態遷移、向きの判定 |
| `csrf.js` | Vitest | ヘッダー付与、401 時の遷移 |
| 画面 | `mvn spring-boot:run` で実機確認 | 下記シナリオ |

実機確認シナリオ:

1. 未ログインで `/top` を開くとログイン画面になる
2. 誤ったパスワードでエラーメッセージが出る
3. 正しい情報でログインでき、ヘッダーに自分の名前が出る
4. 国マスタの登録・更新・削除が動く（CSRF 対応の確認）
5. 銘柄詳細のパラメータ保存、競合の追加・削除が動く
6. 設定画面の「今すぐ更新」が動く
7. ログアウト後、ブラウザの「戻る」で画面を開いても操作できない
8. セッション切れの状態で Ajax 操作をするとログイン画面へ遷移する

---

## 10. 製造順序（案）

| 順 | 作業 | 完了の目安 |
|---|---|---|
| 1 | 課題リストの「要決定事項」を確定 | L-01〜L-03、L-17 に回答がある |
| 2 | `pom.xml` に依存追加、`SecurityConfig`、`UserDao.selectByEmail`、`LoginUserDetailsService` | `mvn clean compile` が通る |
| 3 | `InitialUserRegistrar` で初期ユーザ登録 | `users` に BCrypt ハッシュの行ができる |
| 4 | `chasing_cats.html` 改修、`login.css` / `login.js` 分離、`LoginController` | ログイン・ログアウトができる |
| 5 | `csrf.js` と共通ヘッダー、全テンプレートへの適用 | 既存の登録・更新・削除がすべて動く |
| 6 | 設定画面のログアウトボタン、`settings.test.js` 修正 | `npx vitest run` が通る |
| 7 | テスト追加、実機確認シナリオの実施 | 9 章の全項目が通る |
| 8 | `CLAUDE.md` の更新（認証の記載、テストの記載の修正） | L-21 が解消 |

手順 2 を入れた時点から手順 5 が終わるまで、既存画面の登録・更新・削除は 403 になる。
同じブランチ内で手順 5 まで進めてからマージする。
