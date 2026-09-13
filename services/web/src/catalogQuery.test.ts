import { describe, expect, it } from "vitest";
import { readCatalogQuery, writeCatalogQuery } from "./catalogQuery";

describe("catalogQuery", () => {
  it("drops q shorter than 2 characters", () => {
    const params = new URLSearchParams("q=a&category=Kitchen");
    expect(readCatalogQuery(params)).toMatchObject({ q: "", category: "Kitchen", page: 0 });
  });

  it("keeps filters and omits default sort", () => {
    const next = writeCatalogQuery(new URLSearchParams(), { q: "tent", sort: "name_asc", page: 2 });
    expect(next.get("q")).toBe("tent");
    expect(next.get("sort")).toBeNull();
    expect(next.get("page")).toBe("2");
  });
});
