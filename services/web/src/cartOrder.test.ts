import { describe, expect, it } from "vitest";
import { keepCartOrder } from "./cartOrder";
import { type CartItem } from "./types";

function line(sku: string, qty: number): CartItem {
  return { sku, qty, name: sku, price: "1", stock: 10 };
}

describe("keepCartOrder", () => {
  it("keeps the visible order when quantities change", () => {
    const current = [line("A", 1), line("B", 1), line("C", 1)];
    const incoming = [line("C", 2), line("A", 1), line("B", 3)];
    expect(keepCartOrder(current, incoming).map((item) => item.sku)).toEqual(["A", "B", "C"]);
    expect(keepCartOrder(current, incoming).map((item) => item.qty)).toEqual([1, 3, 2]);
  });
});
