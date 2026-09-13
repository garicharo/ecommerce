import { describe, expect, it } from "vitest";
import { adminTabFromPath } from "./adminTab";

describe("adminTabFromPath", () => {
  it("maps nested routes back to their tab", () => {
    expect(adminTabFromPath("/admin")).toBe("/admin/imports");
    expect(adminTabFromPath("/admin/imports/abc")).toBe("/admin/imports");
    expect(adminTabFromPath("/admin/products/new")).toBe("/admin/products/new");
    expect(adminTabFromPath("/admin/products/RS-001")).toBe("/admin/products");
    expect(adminTabFromPath("/admin/orders")).toBe("/admin/orders");
  });
});
