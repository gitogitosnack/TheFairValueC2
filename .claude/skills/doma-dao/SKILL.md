---
name: doma-dao
description: Doma の DAO インタフェースと SQL ファイルを追加・修正するときに使う。@Select / @Insert / @Update / @Delete の追加、META-INF 配下の SQL ファイル配置、Entity とテーブルのマッピング、DOMA4019 や DOMA4092 などのビルドエラー対処を扱う。「クエリを追加」「DAO にメソッドを足す」「SQL を書き換える」「DOMA4019 が出た」などで発動する。
---

# Doma DAO / SQL の追加・修正

## 前提

- Doma 2.53.1。アノテーションプロセッサ（APT）が**コンパイル時に**SQL ファイルの存在と構文を検証する。
  つまり SQL ファイルの不備はビルドエラーになる。
- DAO は `src/main/java/org/example/web/dao/` に集約。機能パッケージ配下には作らない。
- Entity は `src/main/java/org/example/web/entity/` に集約。テーブルと 1:1。
- 実装クラス（`XxxDaoImpl`）は APT が `target/generated-sources` に自動生成する。手書きしない。
  - 例外：`AnalysisIndicatorDaoImpl` は手書きで存在する。既存のものは触らない。

## SQL ファイルの規約

パスは DAO の完全修飾名をディレクトリに展開したもの＋メソッド名。

```
src/main/resources/META-INF/org/example/web/dao/<DaoName>/<methodName>.sql
```

- **`@Select` は SQL ファイル必須。** `@Insert` / `@Update` / `@Delete` は不要（自動生成）。
- メソッド名とファイル名は完全一致（大文字小文字含む）。オーバーロードは不可。
- 文字コードは UTF-8。

### 書き方

```sql
select
  /*%expand*/*
from
  companies
order by
  id
```

- 列は `/*%expand*/*` で展開させる。Entity のフィールドから列名が生成されるので手書き列挙より安全。
- バインド変数は `/* パラメータ名 */ダミー値` の形式。ダミー値は SQL 単体で実行できる値にする。

```sql
select
  /*%expand*/*
from
  companies
where
  id = /* id */0
```

```sql
select
  /*%expand*/*
from
  financial_statements
where
  company_id = /* companyId */0
  and fiscal_year = /* fiscalYear */2025
  and fiscal_quarter = /* fiscalQuarter */'Q4'
order by
  fiscal_year desc
```

- 条件を任意にしたいときは Doma のディレクティブを使う。

```sql
select
  /*%expand*/*
from
  companies
where
  /*%if code != null */
  code = /* code */'7203'
  /*%end*/
order by
  id
```

- IN 句はリストをそのままバインドする。

```sql
where
  id in /* ids */(0)
```

## DAO の書き方

```java
@Dao
@ConfigAutowireable
public interface CompanyDao {
    @Select
    List<CompanyEntity> selectAll();

    @Select
    Optional<CompanyEntity> selectById(Integer id);

    @Insert
    int insert(CompanyEntity entity);

    @Update
    int update(CompanyEntity entity);

    @Delete
    int delete(CompanyEntity entity);
}
```

- `@Dao` と `@ConfigAutowireable` の両方を付ける（後者がないと Spring から DI できない）。
- 単一件取得の戻り値は `Optional<XxxEntity>`。呼び出し側で
  `orElseThrow(() -> new NotFoundException("..."))` する。
- 集約結果など Entity にならない値は `@Select` でスカラー型（`Integer`, `BigDecimal`, `String`）を返せる。

## 手順

1. Entity が存在するか確認。無ければ先に作る（`@Entity(immutable = false)` ＋ `@Table(name = "...")` ＋
   各フィールドに `@Column(name = "...")`、主キーに `@Id` と `@GeneratedValue(strategy = GenerationType.IDENTITY)`）。
2. DAO にメソッドを追加。
3. `@Select` なら同名の SQL ファイルを正しいパスに作成。
4. `mvn clean compile` で APT を通す。**`compile` だけでなく `clean` を付ける**
   （SQL ファイルの追加が `target/classes` に反映されないことがある）。
5. 実行時の SQL は `logging.level.org.seasar.doma.jdbc.UtilLoggingJdbcLogger=DEBUG` で
   コンソールに出るので、値のバインドまで確認する。

## エラー対処

| エラー | 原因 / 対処 |
|---|---|
| `[DOMA4019] The file "META-INF/.../xxx.sql" is not found in the classpath.` | SQL ファイルのパス・ファイル名がメソッド名と不一致。VS Code だけで出る場合は APT とリソースコピーの順序問題 → `docs/doma4019-vscode.md` の手順に従う。まず `mvn clean compile` で再現するか切り分ける。 |
| `[DOMA4092]` 系（結果マッピング不正） | Entity のフィールドと SELECT した列が対応していない。`/*%expand*/*` を使っているか、`@Column(name=...)` が実列名と合っているか確認。 |
| `[DOMA4181]` / バインド変数エラー | `/* name */` のパラメータ名がメソッド引数名と不一致。 |
| `column ... does not exist`（実行時） | DB 側の実テーブル定義と Entity がずれている。psql で `\d テーブル名` を確認する。 |

## やらないこと

- 生成された `XxxDaoImpl` を編集・コミットしない（`target/` は gitignore 済み）。
- SQL を Java の文字列として組み立てない。必ず SQL ファイルとディレクティブを使う。
- テーブル定義の変更を勝手に実行しない。`ALTER TABLE` / `CREATE TABLE` は文を提示してユーザーに実行を委ねる。
