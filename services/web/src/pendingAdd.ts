import { catalogHome } from "./catalogQuery";
import { api } from "./api";
import { type Product } from "./types";

const KEY = "pendingAdd";

export type PendingAdd = {
  sku: string;
  qty: number;
  returnTo: string;
};

export function shopReturnPath(): string {
  return catalogHome(new URLSearchParams(window.location.search));
}

export function savePendingAdd(sku: string, qty: number, returnTo = shopReturnPath()): void {
  const amount = Math.max(1, Math.floor(qty));
  sessionStorage.setItem(KEY, JSON.stringify({ sku, qty: amount, returnTo }));
}

export function peekPendingAdd(): PendingAdd | null {
  const raw = sessionStorage.getItem(KEY);
  if (!raw) {
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as PendingAdd;
    if (!parsed.sku || !Number.isFinite(parsed.qty) || parsed.qty < 1) {
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

function safeShopPath(path: string): string {
  if (path.startsWith("/") && !path.startsWith("//") && !path.startsWith("/login") && !path.startsWith("/signup")) {
    return path;
  }
  return "/";
}

export async function applyPendingAdd(
  addMore: (sku: string, add: number, stock: number) => Promise<unknown>,
  refresh: () => Promise<void>,
): Promise<string | null> {
  const pending = peekPendingAdd();
  if (!pending) {
    return null;
  }
  sessionStorage.removeItem(KEY);
  await refresh();
  try {
    const product = await api.get<Product>(`/api/products/${encodeURIComponent(pending.sku)}`, {
      auth: false,
      redirectOn401: false,
    });
    await addMore(pending.sku, pending.qty, product.stock);
  } catch {
    // Still send them to the shop; the rail shows whatever landed.
  }
  return safeShopPath(pending.returnTo);
}
