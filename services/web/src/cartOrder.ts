import { type CartItem } from "./types";

export function keepCartOrder(current: CartItem[], incoming: CartItem[]): CartItem[] {
  if (current.length === 0) {
    return incoming;
  }
  const next = new Map(incoming.map((line) => [line.sku, line]));
  const kept = current.flatMap((line) => {
    const fresh = next.get(line.sku);
    return fresh ? [fresh] : [];
  });
  const seen = new Set(kept.map((line) => line.sku));
  return [...kept, ...incoming.filter((line) => !seen.has(line.sku))];
}
