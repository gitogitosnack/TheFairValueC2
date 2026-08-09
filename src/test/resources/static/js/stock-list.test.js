import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import $ from "jquery";

function fakeJqXHR({ succeed, response, xhr }) {
  return {
    done(cb) {
      if (succeed) cb(response);
      return this;
    },
    fail(cb) {
      if (!succeed) cb(xhr || {});
      return this;
    },
  };
}

async function loadModule() {
  vi.resetModules();
  await import("../../../../main/resources/static/js/stock-list.js");
  await new Promise((resolve) => setTimeout(resolve, 0));
}

describe("stock-list.js", () => {
  beforeEach(() => {
    global.$ = $;
    global.jQuery = $;
    document.body.innerHTML = `
      <div id="editModal" class="modal">
        <div class="modal-header"><h2>タイトル</h2></div>
        <span class="close-btn"></span>
        <form id="editForm">
          <input id="modalId" name="id" />
          <input id="modalCode" name="code" />
          <input id="modalName" name="name" />
          <input id="modalMarket_name" name="market_name" />
        </form>
      </div>
      <button id="addStockBtn">追加</button>
      <table>
        <tbody>
          <tr>
            <td>7203</td><td>トヨタ自動車</td><td>東証プライム</td>
            <td class="action-cell">
              <a href="#" class="edit-link" data-id="1"></a>
              <a href="#" class="delete-link" data-id="1"></a>
            </td>
          </tr>
        </tbody>
      </table>`;
    vi.spyOn(window, "alert").mockImplementation(() => {});
    vi.spyOn(window, "confirm").mockImplementation(() => true);
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, reload: vi.fn() },
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  test("新規登録ボタンを押すとフォームがリセットされモーダルタイトルが変わること", async () => {
    $("#modalCode").val("残存値");
    await loadModule();

    $("#addStockBtn").trigger("click");

    expect($("#modalId").val()).toBe("");
    expect($("#modalCode").val()).toBe("");
    expect($(".modal-header h2").text()).toBe("新規銘柄の登録");
  });

  test("編集リンクを押すと行のデータがモーダルの入力欄にセットされること", async () => {
    await loadModule();

    $(".edit-link").trigger("click");

    expect($("#modalId").val()).toBe("1");
    expect($("#modalCode").val()).toBe("7203");
    expect($("#modalName").val()).toBe("トヨタ自動車");
    expect($("#modalMarket_name").val()).toBe("東証プライム");
  });

  test("新規登録の送信でinsertエンドポイントにPOSTされ成功時にリロードされること", async () => {
    const ajaxSpy = vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: true }));
    await loadModule();
    $("#modalId").val("");
    $("#modalCode").val("6758");
    $("#modalName").val("ソニーグループ");
    $("#modalMarket_name").val("東証プライム");

    $("#editForm").trigger("submit");

    const options = ajaxSpy.mock.calls[0][0];
    expect(options.url).toBe("rest_stock_list/insert");
    const sentData = JSON.parse(options.data);
    expect(sentData.code).toBe("6758");
    expect(sentData.market_name).toBe("東証プライム");
    expect(window.alert).toHaveBeenCalledWith("the record updated.");
    expect(window.location.reload).toHaveBeenCalled();
  });

  test("既存IDがあるとき更新エンドポイントにPOSTされること", async () => {
    const ajaxSpy = vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: true }));
    await loadModule();
    $("#modalId").val("1");

    $("#editForm").trigger("submit");

    expect(ajaxSpy.mock.calls[0][0].url).toBe("rest_stock_list/update");
  });

  test("送信が失敗したときエラーアラートを表示すること", async () => {
    vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: false, xhr: {} }));
    await loadModule();

    $("#editForm").trigger("submit");

    expect(window.alert).toHaveBeenCalledWith("Error occurred.");
  });

  test("削除確認でキャンセルしたときAjaxが呼ばれないこと", async () => {
    window.confirm.mockReturnValue(false);
    const ajaxSpy = vi.spyOn($, "ajax");
    await loadModule();

    $(".delete-link").trigger("click");

    expect(ajaxSpy).not.toHaveBeenCalled();
    expect(window.confirm).toHaveBeenCalledWith(
      "銘柄「トヨタ自動車 (7203)」を削除してもよろしいですか？",
    );
  });

  test("削除確認でOKしたときDELETEリクエストが送られること", async () => {
    const ajaxSpy = vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: true }));
    await loadModule();

    $(".delete-link").trigger("click");

    const options = ajaxSpy.mock.calls[0][0];
    expect(options.url).toBe("/rest_stock_list/delete/1");
    expect(options.type).toBe("DELETE");
  });

  test("削除が失敗したとき既定のエラーメッセージを表示すること", async () => {
    vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: false, xhr: {} }));
    await loadModule();

    $(".delete-link").trigger("click");

    expect(window.alert).toHaveBeenCalledWith(
      "削除に失敗しました。時間をおいて再度お試しください。",
    );
  });
});
