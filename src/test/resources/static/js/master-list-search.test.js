import { beforeEach, describe, expect, test, vi } from "vitest";

async function loadModule() {
  vi.resetModules();
  // document.addEventListener("DOMContentLoaded", ...) を実際に発火させると
  // テストをまたいで登録済みハンドラが積み重なってしまうため、
  // 登録されたハンドラを捕まえて直接1回だけ呼び出す。
  const addSpy = vi.spyOn(document, "addEventListener");
  await import("../../../../main/resources/static/js/master-list-search.js");
  const registeredCall = addSpy.mock.calls.find(([type]) => type === "DOMContentLoaded");
  addSpy.mockRestore();
  registeredCall[1]();
}

function buildFixture() {
  document.body.innerHTML = `
    <div class="search-section">
      <input id="searchInput" />
      <button type="button" class="btn-primary">検索</button>
      <button type="button" class="btn-secondary">クリア</button>
    </div>
    <span class="record-count">(2件)</span>
    <div class="list-section">
      <table>
        <thead><tr><th>コード</th><th>名前</th><th>操作</th></tr></thead>
        <tbody>
          <tr><td>JP</td><td>日本</td><td class="action-cell">編集</td></tr>
          <tr><td>US</td><td>アメリカ</td><td class="action-cell">編集</td></tr>
        </tbody>
      </table>
    </div>`;
}

describe("master-list-search.js", () => {
  beforeEach(() => {
    document.body.innerHTML = "";
  });

  test("必須要素が無いとき何もしないこと", async () => {
    await expect(loadModule()).resolves.not.toThrow();
  });

  test("検索ボタンでキーワードに一致する行だけ表示されること", async () => {
    buildFixture();
    await loadModule();
    const rows = document.querySelectorAll("tbody tr");

    document.getElementById("searchInput").value = "アメリカ";
    document.querySelector(".btn-primary").click();

    expect(rows[0].style.display).toBe("none");
    expect(rows[1].style.display).toBe("");
    expect(document.querySelector(".record-count").textContent).toBe("(1件)");
  });

  test("action-cell列の文字列は検索対象に含まれないこと", async () => {
    buildFixture();
    await loadModule();

    document.getElementById("searchInput").value = "編集";
    document.querySelector(".btn-primary").click();

    expect(document.querySelector(".record-count").textContent).toBe("(0件)");
  });

  test("クリアボタンで検索欄が空になり全行表示に戻ること", async () => {
    buildFixture();
    await loadModule();
    const input = document.getElementById("searchInput");
    input.value = "日本";
    document.querySelector(".btn-primary").click();

    document.querySelector(".btn-secondary").click();

    expect(input.value).toBe("");
    const dataRows = document.querySelectorAll("tbody tr:not(.search-no-result-row)");
    dataRows.forEach((row) => expect(row.style.display).toBe(""));
    expect(document.querySelector(".record-count").textContent).toBe("(2件)");
  });

  test("一致件数が0件のとき一致なしメッセージ行が表示されること", async () => {
    buildFixture();
    await loadModule();

    document.getElementById("searchInput").value = "存在しないキーワード";
    document.querySelector(".btn-primary").click();

    const noResultRow = document.querySelector(".search-no-result-row");
    expect(noResultRow.style.display).toBe("");
  });

  test("Enterキーでも検索が実行されること", async () => {
    buildFixture();
    await loadModule();
    const input = document.getElementById("searchInput");
    input.value = "日本";

    input.dispatchEvent(new KeyboardEvent("keydown", { key: "Enter" }));

    expect(document.querySelector(".record-count").textContent).toBe("(1件)");
  });
});
