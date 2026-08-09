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
  await import("../../../../main/resources/static/js/country.js");
  // $(document).ready はドキュメントが読み込み済みの場合 setTimeout(0) で遅延実行されるため待つ
  await new Promise((resolve) => setTimeout(resolve, 0));
}

describe("country.js", () => {
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
        </form>
      </div>
      <button id="addCountryBtn">追加</button>
      <table>
        <tbody>
          <tr>
            <td>1</td><td>JP</td><td>日本</td>
            <td class="action-cell">
              <a href="#" class="edit-link" data-id="1"></a>
              <a href="#" class="delete-link" data-id="1"></a>
            </td>
          </tr>
        </tbody>
      </table>`;
    vi.spyOn(window, "alert").mockImplementation(() => {});
    vi.spyOn(window, "confirm").mockImplementation(() => true);
    // jsdom の window.location.reload は再定義不可のため location ごと差し替える
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

    $("#addCountryBtn").trigger("click");

    expect($("#modalId").val()).toBe("");
    expect($("#modalCode").val()).toBe("");
    expect($(".modal-header h2").text()).toBe("国の登録");
  });

  test("編集リンクを押すと行のデータがモーダルの入力欄にセットされること", async () => {
    await loadModule();

    $(".edit-link").trigger("click");

    expect($("#modalId").val()).toBe("1");
    expect($("#modalCode").val()).toBe("JP");
    expect($("#modalName").val()).toBe("日本");
  });

  test("新規登録の送信でinsertエンドポイントにPOSTされ成功時にリロードされること", async () => {
    const ajaxSpy = vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: true }));
    await loadModule();
    $("#modalId").val("");
    $("#modalCode").val("US");
    $("#modalName").val("アメリカ");

    $("#editForm").trigger("submit");

    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const options = ajaxSpy.mock.calls[0][0];
    expect(options.url).toBe("rest_countries/insert");
    expect(options.type).toBe("POST");
    const sentData = JSON.parse(options.data);
    expect(sentData.code).toBe("US");
    expect(sentData.name).toBe("アメリカ");
    expect(window.alert).toHaveBeenCalledWith("the record updated.");
    expect(window.location.reload).toHaveBeenCalled();
  });

  test("既存IDがあるとき更新エンドポイントにPOSTされること", async () => {
    const ajaxSpy = vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: true }));
    await loadModule();
    $("#modalId").val("1");

    $("#editForm").trigger("submit");

    const options = ajaxSpy.mock.calls[0][0];
    expect(options.url).toBe("rest_countries/update");
    expect(JSON.parse(options.data).id).toBe("1");
  });

  test("送信が失敗したときエラーアラートを表示すること", async () => {
    vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: false, xhr: {} }));
    await loadModule();

    $("#editForm").trigger("submit");

    expect(window.alert).toHaveBeenCalledWith("Error occurred.");
    expect(window.location.reload).not.toHaveBeenCalled();
  });

  test("削除確認でキャンセルしたときAjaxが呼ばれないこと", async () => {
    window.confirm.mockReturnValue(false);
    const ajaxSpy = vi.spyOn($, "ajax");
    await loadModule();

    $(".delete-link").trigger("click");

    expect(ajaxSpy).not.toHaveBeenCalled();
  });

  test("削除確認でOKしたときDELETEリクエストが送られること", async () => {
    const ajaxSpy = vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: true }));
    await loadModule();

    $(".delete-link").trigger("click");

    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const options = ajaxSpy.mock.calls[0][0];
    expect(options.url).toBe("/rest_countries/delete/1");
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
