import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "./AuthProvider";
import { CartLineQty } from "./components/CartLineQty";
import { ProductImage } from "./components/ProductImage";
import { StatusBanner } from "./components/StatusBanner";
import { HttpError, api } from "./api";
import { keepCartOrder } from "./cartOrder";
import { money, productPath, type Cart, type CartItem } from "./types";

type AddResult = {
  next: number;
  added: number;
  remaining: number;
};

type CartApi = {
  items: CartItem[];
  status: "idle" | "loading" | "ok" | "error";
  error: string;
  count: number;
  subtotal: number;
  refresh: () => Promise<void>;
  setQty: (sku: string, qty: number) => Promise<void>;
  addMore: (sku: string, add: number, stock: number) => Promise<AddResult>;
};

const CartContext = createContext<CartApi | null>(null);

export function useCart(): CartApi {
  const ctx = useContext(CartContext);
  if (!ctx) {
    throw new Error("useCart needs CartProvider");
  }
  return ctx;
}

export function cartLine(items: CartItem[], sku: string): CartItem | undefined {
  return items.find((item) => item.sku === sku);
}

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([]);
  const [status, setStatus] = useState<CartApi["status"]>("idle");
  const [error, setError] = useState("");
  const { user, status: authStatus } = useAuth();
  const loggedIn = user !== null;

  const itemsRef = useRef(items);
  itemsRef.current = items;
  const wanted = useRef<Record<string, number>>({});
  const flushing = useRef<Record<string, boolean>>({});

  const refresh = useCallback(async () => {
    if (authStatus === "loading") {
      return;
    }
    if (!loggedIn) {
      setItems([]);
      setError("");
      setStatus("ok");
      return;
    }
    if (itemsRef.current.length === 0) {
      setStatus("loading");
    }
    try {
      const cart = await api.get<Cart>("/api/cart", { redirectOn401: false });
      setItems((current) => keepCartOrder(current, cart.items ?? []));
      setError("");
      setStatus("ok");
    } catch (err: unknown) {
      if (err instanceof HttpError && (err.status === 401 || err.status === 403)) {
        setItems([]);
        setError("");
        setStatus("ok");
        return;
      }
      setError(err instanceof Error ? err.message : "Could not load cart");
      setStatus("error");
    }
  }, [authStatus, loggedIn]);

  const setQty = useCallback(async (sku: string, qty: number) => {
    const next = Math.max(0, Math.floor(qty));
    wanted.current[sku] = next;
    setItems((current) => {
      if (next <= 0) {
        return current.filter((line) => line.sku !== sku);
      }
      return current.map((line) => (line.sku === sku ? { ...line, qty: next } : line));
    });
    if (flushing.current[sku]) {
      return;
    }
    flushing.current[sku] = true;
    try {
      while (Object.hasOwn(wanted.current, sku)) {
        const pending = wanted.current[sku];
        delete wanted.current[sku];
        if (pending === undefined) {
          break;
        }
        const cart = await api.put<Cart>("/api/cart/items", { sku, qty: pending });
        if (!Object.hasOwn(wanted.current, sku)) {
          setItems((current) => keepCartOrder(current, cart.items ?? []));
        }
      }
    } catch (err: unknown) {
      delete wanted.current[sku];
      await refresh();
      throw err;
    } finally {
      flushing.current[sku] = false;
    }
  }, [refresh]);

  const addMore = useCallback(
    async (sku: string, add: number, stock: number) => {
      const current = itemsRef.current.find((item) => item.sku === sku)?.qty ?? 0;
      const room = Math.max(0, stock - current);
      if (room === 0) {
        return { next: current, added: 0, remaining: 0 };
      }
      const added = Math.min(Math.max(1, Math.floor(add)), room);
      const next = current + added;
      await setQty(sku, next);
      return { next, added, remaining: Math.max(0, stock - next) };
    },
    [setQty],
  );

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const count = items.reduce((sum, line) => sum + line.qty, 0);
  const subtotal = items.reduce((sum, line) => sum + Number(line.price) * line.qty, 0);

  const value = useMemo(
    () => ({ items, status, error, count, subtotal, refresh, setQty, addMore }),
    [items, status, error, count, subtotal, refresh, setQty, addMore],
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function CartRail() {
  const { items, status, error, subtotal, setQty } = useCart();

  function change(sku: string, qty: number, stock: number) {
    void setQty(sku, Math.min(qty, stock)).catch(() => undefined);
  }

  return (
    <aside className="cart-rail" aria-label="Cart">
      <div className="cart-rail-head">
        <h2>{items.length > 0 ? <Link to="/cart">Cart</Link> : "Cart"}</h2>
      </div>
      {status === "loading" && items.length === 0 ? <p className="muted">Loading…</p> : null}
      {status === "error" ? <StatusBanner error={error} /> : null}
      {status === "ok" && items.length === 0 ? (
        <p className="muted">Your cart is empty.</p>
      ) : null}
      {items.length > 0 ? (
        <ul className="cart-lines">
          {items.map((line) => {
            const href = productPath(line.sku);
            return (
              <li key={line.sku} className="cart-line">
                <Link className="thumb-link" to={href} aria-label={line.name}>
                  <ProductImage className="thumb" sku={line.sku} name={line.name} />
                </Link>
                <div className="cart-line-copy">
                  <Link className="cart-line-name" to={href}>
                    {line.name}
                  </Link>
                  <div className="cart-line-meta">
                    <CartLineQty
                      name={line.name}
                      qty={line.qty}
                      stock={line.stock}
                      onQty={(qty) => change(line.sku, qty, line.stock)}
                      onRemove={() => change(line.sku, 0, line.stock)}
                    />
                    <span className="cart-line-total">{money(Number(line.price) * line.qty)}</span>
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      ) : null}
      {items.length > 0 ? (
        <div className="cart-rail-foot">
          <p className="cart-subtotal">
            Cart total <strong>{money(subtotal)}</strong>
          </p>
          <Link className="btn" to="/checkout">
            Checkout
          </Link>
        </div>
      ) : null}
    </aside>
  );
}

export function useShowCartRail(): boolean {
  const { user } = useAuth();
  const { pathname } = useLocation();
  if (!user) {
    return false;
  }
  return !(
    pathname.startsWith("/login") ||
    pathname.startsWith("/signup") ||
    pathname.startsWith("/cart") ||
    pathname.startsWith("/checkout") ||
    pathname.startsWith("/orders") ||
    pathname.startsWith("/admin")
  );
}
