// マスタ一覧・銘柄一覧の共通処理：検索ボタン / クリアボタン
//
// 各一覧ページ（country-list, currency-list, industry-list,
// valuation-model-list, stock-list）は同じマークアップ構造
// （.search-section 内に #searchInput と .btn-primary（検索）/ .btn-secondary（クリア）、
// .list-section 内のテーブルに検索対象行）を共有しているため、1本のスクリプトで
// クライアントサイドの部分一致フィルタとして動かす。
document.addEventListener("DOMContentLoaded", function () {
  const searchInput = document.getElementById("searchInput");
  const searchSection = document.querySelector(".search-section");
  const table = document.querySelector(".list-section table");

  if (!searchInput || !searchSection || !table) {
    return;
  }

  const searchButton = searchSection.querySelector(".btn-primary");
  const clearButton = searchSection.querySelector(".btn-secondary");
  const tbody = table.querySelector("tbody");
  const recordCountEl = document.querySelector(".record-count");
  const columnCount = table.querySelectorAll("thead th").length || 1;

  if (!tbody) {
    return;
  }

  // 「登録が0件です」のプレースホルダー行は検索対象から除外する
  const dataRows = Array.from(tbody.querySelectorAll("tr")).filter(
    (row) => !row.querySelector(".empty-cell"),
  );

  // 検索結果が0件のときに表示するメッセージ行
  const noResultRow = document.createElement("tr");
  noResultRow.className = "search-no-result-row";
  noResultRow.style.display = "none";
  const noResultCell = document.createElement("td");
  noResultCell.className = "empty-cell";
  noResultCell.colSpan = columnCount;
  noResultCell.innerHTML =
    '<i class="fa-regular fa-folder-open"></i>検索条件に一致するデータがありません';
  noResultRow.appendChild(noResultCell);
  tbody.appendChild(noResultRow);

  // 「編集」「削除」リンクの列は検索対象から除いたテキストを作る
  function rowSearchText(row) {
    return Array.from(row.querySelectorAll("td"))
      .filter((cell) => !cell.classList.contains("action-cell"))
      .map((cell) => cell.textContent)
      .join(" ")
      .toLowerCase();
  }

  function applyFilter() {
    const keyword = searchInput.value.trim().toLowerCase();
    let visibleCount = 0;

    dataRows.forEach((row) => {
      const matched = keyword === "" || rowSearchText(row).includes(keyword);
      row.style.display = matched ? "" : "none";
      if (matched) {
        visibleCount++;
      }
    });

    noResultRow.style.display =
      dataRows.length > 0 && visibleCount === 0 ? "" : "none";

    if (recordCountEl) {
      recordCountEl.textContent = `(${visibleCount}件)`;
    }
  }

  if (searchButton) {
    searchButton.addEventListener("click", function (event) {
      event.preventDefault();
      applyFilter();
    });
  }

  if (clearButton) {
    clearButton.addEventListener("click", function (event) {
      event.preventDefault();
      searchInput.value = "";
      applyFilter();
    });
  }

  // Enterキーでも検索を実行できるようにする
  searchInput.addEventListener("keydown", function (event) {
    if (event.key === "Enter") {
      event.preventDefault();
      applyFilter();
    }
  });
});
