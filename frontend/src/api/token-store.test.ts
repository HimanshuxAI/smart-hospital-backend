import { beforeEach, describe, expect, it } from "vitest";

import { REFRESH_TOKEN_KEY, createTokenStore } from "./token-store";

describe("createTokenStore", () => {
  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });

  it("keeps the access token in memory and the refresh token in sessionStorage", () => {
    const store = createTokenStore(sessionStorage);

    store.setAccessToken("access-secret");
    store.setRefreshToken("refresh-secret");

    expect(store.getAccessToken()).toBe("access-secret");
    expect(store.getRefreshToken()).toBe("refresh-secret");
    expect(sessionStorage.getItem(REFRESH_TOKEN_KEY)).toBe("refresh-secret");
    expect(sessionStorage.getItem("access-secret")).toBeNull();
    expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();
    expect(JSON.stringify(sessionStorage)).not.toContain("access-secret");
    expect(JSON.stringify(localStorage)).not.toContain("access-secret");
  });

  it("clears both tokens", () => {
    const store = createTokenStore(sessionStorage);
    store.setAccessToken("access-secret");
    store.setRefreshToken("refresh-secret");

    store.clear();

    expect(store.getAccessToken()).toBeNull();
    expect(store.getRefreshToken()).toBeNull();
    expect(sessionStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();
  });
});
