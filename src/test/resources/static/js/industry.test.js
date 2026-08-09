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
  await import("../../../../main/resources/static/js/industry.js");
  await new Promise((resolve) => setTimeout(resolve, 0));
}

describe("industry.js", () => {
  beforeEach(() => {
    global.$ = $;
    global.jQuery = $;
    document.body.innerHTML = `
      <div id="editModal" class="modal">
        <div class="modal-header"><h2>タイトル</h2></div>
        <span class="close-btn"></span>
        <form id="editForm">
          <input id="modalId" name="id" />
          <input id="modalName" name="name" />
          <input id="modalSectorName" name="sectorName" />
          <input id="modalAvgPer" name="avgPer" />
          <input id="modalDescription" name="description" />
        </form>
      </div>
      <button id="addIndustryBtn">追加</button>
      <table>
        <tbody>
          <tr>
            <td>1</td><td>自動車</td><td>輸送用機器</td><td>12.5</td><td>説明文</td>
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
    $("#modalName").val("残存値");
    await loadModule();

    $("#addIndustryBtn").trigger("click");

    expect($("#modalId").val()).toBe("");
    expect($("#modalName").val()).toBe("");
    expect($(".modal-header h2").text()).toBe("業種の登録");
  });

  test("編集リンクを押すと行のデータがモーダルの入力欄にセットされること", async () => {
    await loadModule();

    $(".edit-link").trigger("click");

    expect($("#modalId").val()).toBe("1");
    expect($("#modalName").val()).toBe("自動車");
    expect($("#modalSectorName").val()).toBe("輸送用機器");
    expect($("#modalAvgPer").val()).toBe("12.5");
    expect($("#modalDescription").val()).toBe("説明文");
    expect($(".modal-header h2").text()).toBe("業種情報の編集");
  });

  test("新規登録の送信でinsertエンドポイントにPOSTされ成功時にリロードされること", async () => {
    const ajaxSpy = vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: true }));
    await loadModule();
    $("#modalId").val("");
    $("#modalName").val("小売業");

    $("#editForm").trigger("submit");

    const options = ajaxSpy.mock.calls[0][0];
    expect(options.url).toBe("rest_industries/insert");
    expect(JSON.parse(options.data).name).toBe("小売業");
    expect(window.alert).toHaveBeenCalledWith("保存しました。");
    expect(window.location.reload).toHaveBeenCalled();
  });

  test("既存IDがあるとき更新エンドポイントにPOSTされること", async () => {
    const ajaxSpy = vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: true }));
    await loadModule();
    $("#modalId").val("1");

    $("#editForm").trigger("submit");

    const options = ajaxSpy.mock.calls[0][0];
    expect(options.url).toBe("rest_industries/update");
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
      "業種「自動車 (輸送用機器)」を削除してもよろしいですか？",
    );
  });

  test("削除確認でOKしたときDELETEリクエストが送られること", async () => {
    const ajaxSpy = vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: true }));
    await loadModule();

    $(".delete-link").trigger("click");

    const options = ajaxSpy.mock.calls[0][0];
    expect(options.url).toBe("/rest_industries/delete/1");
    expect(options.type).toBe("DELETE");
  });

  test("削除が失敗したときエラーアラートを表示すること", async () => {
    vi.spyOn($, "ajax").mockReturnValue(fakeJqXHR({ succeed: false, xhr: {} }));
    await loadModule();

    $(".delete-link").trigger("click");

    expect(window.alert).toHaveBeenCalledWith("削除に失敗しました。");
  });
});
