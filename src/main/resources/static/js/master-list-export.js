// マスタ一覧・銘柄一覧の共通処理：Excel出力ボタン
//
// 各一覧ページ（country-list, currency-list, industry-list,
// valuation-model-list, stock-list）の .btn-export に
// data-export-url でサーバ側のダウンロードURLを持たせておき、
// クリックされたらそのURLへ遷移してブラウザにファイルをダウンロードさせる。
document.addEventListener("DOMContentLoaded", function () {
  const exportButton = document.querySelector(".btn-export");

  if (!exportButton) {
    return;
  }

  exportButton.addEventListener("click", function (event) {
    event.preventDefault();

    const exportUrl = exportButton.dataset.exportUrl;
    if (!exportUrl) {
      return;
    }

    window.location.href = exportUrl;
  });
});
