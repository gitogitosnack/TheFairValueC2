import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";

async function loadModule() {
  vi.resetModules();
  // document.addEventListener("DOMContentLoaded", ...) を実際に発火させると
  // テストをまたいで登録済みハンドラが積み重なってしまうため、
  // 登録されたハンドラを捕まえて直接1回だけ呼び出す。
  const addSpy = vi.spyOn(document, "addEventListener");
  await import("../../../../main/resources/static/js/settings.js");
  const registeredCall = addSpy.mock.calls.find(([type]) => type === "DOMContentLoaded");
  addSpy.mockRestore();
  registeredCall[1]();
}

describe("settings.js", () => {
  beforeEach(() => {
    document.body.innerHTML = "";
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  test("保存ボタンを押すと対応するトーストが表示されること", async () => {
    document.body.innerHTML = `
      <button data-role="save-profile">保存</button>
      <div data-role="profile-toast"></div>`;
    await loadModule();

    document.querySelector("[data-role='save-profile']").click();

    expect(document.querySelector("[data-role='profile-toast']").classList.contains("show")).toBe(true);
  });

  test("対応するトースト要素が無いとき何も起きないこと", async () => {
    document.body.innerHTML = `<button data-role="save-profile">保存</button>`;
    await loadModule();

    expect(() => document.querySelector("[data-role='save-profile']").click()).not.toThrow();
  });

  test("2秒後にトーストが自動的に消えること", async () => {
    vi.useFakeTimers();
    document.body.innerHTML = `
      <button data-role="save-profile">保存</button>
      <div data-role="profile-toast"></div>`;
    await loadModule();
    const toast = document.querySelector("[data-role='profile-toast']");

    document.querySelector("[data-role='save-profile']").click();
    expect(toast.classList.contains("show")).toBe(true);

    vi.advanceTimersByTime(2000);

    expect(toast.classList.contains("show")).toBe(false);
  });

  test("アクセントカラースウォッチをクリックすると選択状態が1つだけになること", async () => {
    document.body.innerHTML = `
      <div data-role="color-swatches">
        <span class="color-swatch selected" data-color="green"></span>
        <span class="color-swatch" data-color="blue"></span>
      </div>`;
    await loadModule();
    const swatches = document.querySelectorAll(".color-swatch");

    swatches[1].click();

    expect(swatches[0].classList.contains("selected")).toBe(false);
    expect(swatches[1].classList.contains("selected")).toBe(true);
  });

  test("表示モード切替ボタンをクリックすると選択状態が1つだけになること", async () => {
    document.body.innerHTML = `
      <div data-role="mode-toggle">
        <button class="mode-toggle-btn active" data-mode="dark"></button>
        <button class="mode-toggle-btn" data-mode="light"></button>
      </div>`;
    await loadModule();
    const buttons = document.querySelectorAll(".mode-toggle-btn");

    buttons[1].click();

    expect(buttons[0].classList.contains("active")).toBe(false);
    expect(buttons[1].classList.contains("active")).toBe(true);
  });

  test("ログアウトボタンを押すと銘柄一覧に遷移すること", async () => {
    document.body.innerHTML = `<button id="btn-logout-settings">ログアウト</button>`;
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, href: "" },
    });
    await loadModule();

    document.getElementById("btn-logout-settings").click();

    expect(window.location.href).toBe("/top");
  });
});
