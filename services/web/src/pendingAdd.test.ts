import { afterEach, describe, expect, it } from "vitest";
import { peekPendingAdd, savePendingAdd } from "./pendingAdd";

afterEach(() => {
  sessionStorage.clear();
});

describe("pendingAdd", () => {
  it("stores sku and qty for after login", () => {
    savePendingAdd("CT-005", 2, "/?category=Outdoors");
    expect(peekPendingAdd()).toEqual({ sku: "CT-005", qty: 2, returnTo: "/?category=Outdoors" });
  });
});
