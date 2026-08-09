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
  await import("../../../../main/resources/static/js/valuation-model.js");
  await new Promise((resolve) => setTimeout(resolve, 0));
}

describe("valuation-model.js", () => {
  beforeEach(() => {
    global.$ = $;
    global.jQuery = $;
    document.body.innerHTML = `
      <div id="editModal" class="modal">
        <div class="modal-header"><h2>タイトル</h2></div>
        <span class="close-btn"></span>
        <form id="editForm">
          <input id="modalId" name="id" />
          <input id="modalModelName" name="modelName" />
          <input id="modalFormulaDescription" name="formulaDescription" />
        </form>
      </div>
      <button id="addModelBtn">追加</button>
      <table>
        <tbody>
          <tr>
            <td>1</td><td>DCF法</td><td>将来キャッシュフローを割り引く方法</td>
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
    $("#modalModelName").val("残存値");
    await loadModule();

    $("#addModelBtn").trigger("click");

    expect($("#modalId").val()).toBe("");
    expect($("#modalModelName").val()).toBe("");
    expect($(".modal-header h2").text()).toBe("評価モデルの登録");
  });

  test("編集リンクを押すと行のデータがモーダルの入力欄にセットされること", async () => {
    await loadModule();

    $(".edit-link").trigger("click");

    expect($("#modalId").val()).toBe("1");
    expect($("#modalModelName").val()).toBe("DCF法");
    expect($("#modalFormulaDescription").val()).toBe("将来キャッシュフローを割り引く方法");
    expect($(".modal-header h2").text()).toBe("評価モデル情報の編集");
  });

  test("新規登録の送信でinsertエンドポイントにPOSTされ成功時にリロードされること", async () => {
    const ajaxSpy = vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: true }));
    await loadModule();
    $("#modalId").val("");
    $("#modalModelName").val("PERマルチプル法");

    $("#editForm").trigger("submit");

    const options = ajaxSpy.mock.calls[0][0];
    expect(options.url).toBe("rest_valuation_models/insert");
    expect(JSON.parse(options.data).modelName).toBe("PERマルチプル法");
    expect(window.alert).toHaveBeenCalledWith("保存しました。");
    expect(window.location.reload).toHaveBeenCalled();
  });

  test("既存IDがあるとき更新エンドポイントにPOSTされること", async () => {
    const ajaxSpy = vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: true }));
    await loadModule();
    $("#modalId").val("1");

    $("#editForm").trigger("submit");

    expect(ajaxSpy.mock.calls[0][0].url).toBe("rest_valuation_models/update");
  });

  test("送信が失敗したときエラーアラートを表示すること", async () => {
    vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: false, xhr: {} }));
    await loadModule();

    $("#editForm").trigger("submit");

    expect(window.alert).toHaveBeenCalledWith("保存に失敗しました。");
  });

  test("削除確認でキャンセルしたときAjaxが呼ばれないこと", async () => {
    window.confirm.mockReturnValue(false);
    const ajaxSpy = vi.spyOn($, "ajax");
    await loadModule();

    $(".delete-link").trigger("click");

    expect(ajaxSpy).not.toHaveBeenCalled();
    expect(window.confirm).toHaveBeenCalledWith(
      "評価モデル「DCF法」を削除してもよろしいですか？",
    );
  });

  test("削除確認でOKしたときDELETEリクエストが送られること", async () => {
    const ajaxSpy = vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: true }));
    await loadModule();

    $(".delete-link").trigger("click");

    const options = ajaxSpy.mock.calls[0][0];
    expect(options.url).toBe("/rest_valuation_models/delete/1");
    expect(options.type).toBe("DELETE");
  });

  test("削除が失敗したときエラーアラートを表示すること", async () => {
    vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: false, xhr: {} }));
    await loadModule();

    $(".delete-link").trigger("click");

    expect(window.alert).toHaveBeenCalledWith("削除に失敗しました。");
  });
});
