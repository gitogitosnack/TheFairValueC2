import { beforeEach, describe, expect, test, vi } from "vitest";

async function loadModule() {
  vi.resetModules();
  // document.addEventListener("DOMContentLoaded", ...) を実際に発火させると
  // テストをまたいで登録済みハンドラが積み重なってしまうため、
  // 登録されたハンドラを捕まえて直接1回だけ呼び出す。
  const addSpy = vi.spyOn(document, "addEventListener");
  await import("../../../../main/resources/static/js/master-list-export.js");
  const registeredCall = addSpy.mock.calls.find(([type]) => type === "DOMContentLoaded");
  addSpy.mockRestore();
  registeredCall[1]();
}

describe("master-list-export.js", () => {
  beforeEach(() => {
    document.body.innerHTML = "";
  });

  test("エクスポートボタンが無いとき何もしないこと", async () => {
    await expect(loadModule()).resolves.not.toThrow();
  });

  test("エクスポートボタンを押すとdata-export-urlへ遷移すること", async () => {
    document.body.innerHTML =
      '<button class="btn-export" data-export-url="/rest_countries/export">出力</button>';
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, href: "" },
    });
    await loadModule();

    document.querySelector(".btn-export").click();

    expect(window.location.href).toBe("/rest_countries/export");
  });

  test("data-export-urlが無いとき遷移しないこと", async () => {
    document.body.innerHTML = '<button class="btn-export">出力</button>';
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, href: "" },
    });
    await loadModule();

    document.querySelector(".btn-export").click();

    expect(window.location.href).toBe("");
  });
});
