# 画面側テンプレート（Thymeleaf ＋ jQuery）

プレースホルダは `java-templates.md` と同じ。
加えて `{{機能名}}` = 画面に出す日本語名（例「セクタ」）。

`th:href` は kebab-case の画面 URL、Ajax の `url` は snake_case の REST URL を使う。

前提：

- CSS は既存の `style.css` / `stock-list.css` / `stock-list-modals.css` を再利用する。新規 CSS は作らない。
- jQuery は CDN 読み込み。ES モジュールやビルドは使わない。
- ナビゲーションバーは全マスタ画面で同じ。新しいマスタを追加したら**既存の全画面の `nav-menu` にもリンクを追記する**。

---

## 1. Thymeleaf テンプレート

`src/main/resources/templates/{{feature}}-list/{{feature}}-list.html`

```html
<!DOCTYPE html>
<html lang="ja" xmlns:th="http://www.thymeleaf.org">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{機能名}}マスタ一覧</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/destyle.css@3.0.2/destyle.css">
    <link rel="stylesheet" th:href="@{/css/style.css}">
    <link rel="stylesheet" th:href="@{/css/stock-list.css}">
    <link rel="stylesheet" th:href="@{/css/stock-list-modals.css}">
</head>

<body>
    <nav class="navbar">
        <div class="nav-container">
            <div class="nav-logo"><a th:href="@{/top}">StockAnalyzer</a></div>
            <ul class="nav-menu">
                <li><a th:href="@{/top}">銘柄一覧</a></li>
                <li><a th:href="@{/countries}">国マスタ</a></li>
                <li><a th:href="@{/currencies}">通貨マスタ</a></li>
                <li><a th:href="@{/industries}">業種マスタ</a></li>
                <li><a th:href="@{/valuation-models}">評価モデル</a></li>
                <li><a th:href="@{/{{features}}}" class="active">{{機能名}}マスタ</a></li>
                <li><a href="#">設定</a></li>
            </ul>
            <div class="nav-user">
                <span class="user-name">テスト太郎 様</span>
                <button class="btn-logout">ログアウト</button>
            </div>
        </div>
    </nav>

    <div class="container main-content">
        <header class="page-header">
            <h1>{{機能名}}マスタ一覧</h1>
        </header>

        <section class="search-section">
            <div class="search-box">
                <input type="text" placeholder="{{機能名}}名・コードを入力（部分一致）" id="searchInput">
                <button class="btn btn-primary">検索</button>
                <button class="btn btn-secondary">クリア</button>
            </div>
            <div class="action-buttons">
                <button class="btn btn-export">Excel出力</button>
            </div>
        </section>

        <section class="list-section">
            <div class="list-header">
                <h2>登録{{機能名}}一覧</h2>
                <button id="add{{Feature}}Btn" class="btn btn-success">+ 新規{{機能名}}登録</button>
            </div>

            <table class="stock-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>コード</th>
                        <th>{{機能名}}名</th>
                        <th></th>
                        <th></th>
                    </tr>
                </thead>
                <tbody>
                    <tr th:each="item : ${items}">
                        <td th:text="${item.id}">1</td>
                        <td th:text="${item.code}">CODE</td>
                        <td th:text="${item.name}">名称</td>
                        <td><a href="#" class="operation-link edit-link">編集</a></td>
                        <td><a href="#" class="operation-link delete-link" th:data-id="${item.id}">削除</a></td>
                    </tr>
                </tbody>
            </table>
        </section>
    </div>

    <!--  START: Modal for edit display  -->
    <div id="editModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>{{機能名}}情報の編集</h2>
                <span class="close-btn">&times;</span>
            </div>
            <form id="editForm">
                <input type="hidden" id="modalId" name="id">
                <div class="modal-body">
                    <div class="form-group">
                        <label>{{機能名}}コード</label>
                        <input type="text" id="modalCode" name="code" required>
                    </div>
                    <div class="form-group">
                        <label>{{機能名}}名</label>
                        <input type="text" id="modalName" name="name" required>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary close-modal">キャンセル</button>
                    <button type="submit" class="btn btn-primary">保存</button>
                </div>
            </form>
        </div>
    </div>
    <!--  END: Modal for edit display  -->

    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.7.1/jquery.min.js"></script>
    <script th:src="@{/js/{{feature}}.js}"></script>
</body>

</html>
```

## 2. JavaScript

`src/main/resources/static/js/{{feature}}.js`

REST の URL は先頭 `/` 付きの絶対パスで書く（`country.js` の insert/update は相対パスで、
サブパス配下だと壊れる。新規コードでは絶対パスに揃える）。

```js
$(document).ready(function () {

    const $modal = $('#editModal');

    // 新規登録ボタン
    $('#add{{Feature}}Btn').on('click', function () {
        $('#editForm')[0].reset();
        $('#modalId').val('');
        $('.modal-header h2').text('{{機能名}}の登録');
        $modal.fadeIn(200);
    });

    // モーダル外クリック・閉じるボタン
    $(window).on('click', function (event) {
        if ($(event.target).is($modal)) {
            $modal.fadeOut(200);
        }
    });

    $('.close-btn, .close-modal').on('click', function () {
        $modal.fadeOut(200);
    });

    // 登録 / 更新
    $('#editForm').on('submit', function (event) {
        event.preventDefault();

        const id = $('#modalId').val();
        const isNew = (id === '' || id === null);
        const targetUrl = isNew ? '/rest_{{features}}/insert' : '/rest_{{features}}/update';

        const formData = {
            id: isNew ? null : Number(id)
            , code: $('#modalCode').val()
            , name: $('#modalName').val()
        };

        $.ajax({
            url: targetUrl
            , type: 'POST'
            , contentType: 'application/json'
            , data: JSON.stringify(formData)
        })
            .done(function () {
                alert(isNew ? '登録が完了しました。' : '更新が完了しました。');
                location.reload();
            })
            .fail(function (xhr) {
                console.error('Error:', xhr);
                alert(xhr.responseText || '保存に失敗しました。');
            });

        $modal.fadeOut(200);
    });

    // 削除
    $('.delete-link').on('click', function (event) {
        event.preventDefault();

        const $row = $(this).closest('tr');
        const id = $(this).data('id');
        const code = $row.find('td:eq(1)').text().trim();
        const name = $row.find('td:eq(2)').text().trim();

        if (!confirm(`{{機能名}}「${name} (${code})」を削除してもよろしいですか？`)) {
            return false;
        }

        $.ajax({
            url: '/rest_{{features}}/delete/' + id
            , type: 'DELETE'
        })
            .done(function () {
                $row.fadeOut(400, function () {
                    $(this).remove();
                    alert('削除が完了しました。');
                });
            })
            .fail(function (xhr) {
                console.error('Error:', xhr);
                alert(xhr.responseText || '削除に失敗しました。');
            });
    });

    // 編集
    $('.edit-link').on('click', function (event) {
        event.preventDefault();

        const $row = $(this).closest('tr');

        $('.modal-header h2').text('{{機能名}}情報の編集');
        $('#modalId').val($row.find('td:eq(0)').text().trim());
        $('#modalCode').val($row.find('td:eq(1)').text().trim());
        $('#modalName').val($row.find('td:eq(2)').text().trim());

        $modal.fadeIn(200);
    });

});
```

## 3. 仕上げ

```powershell
npx prettier --write "src/main/resources/static/js/{{feature}}.js"
```
