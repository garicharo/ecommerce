import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { CartLineQty } from "../components/CartLineQty";
import { Layout } from "../Layout";
import { ProductImage } from "../components/ProductImage";
import { StatusBanner } from "../components/StatusBanner";
import { api } from "../api";
import { useCart } from "../cart";
import { money, productPath } from "../types";

const STOCK_KEY = "stockError";

type StockDetail = { sku?: string; requested?: number; available?: number };

export function CartPage() {
  const { items, status, setQty, refresh } = useCart();
  const [error, setError] = useState("");
  const [booting, setBooting] = useState(true);

  useEffect(() => {
    let cancelled = false;
    async function boot() {
      const raw = sessionStorage.getItem(STOCK_KEY);
      if (raw) {
        sessionStorage.removeItem(STOCK_KEY);
        let details: StockDetail[] = [];
        try {
          details = JSON.parse(raw) as StockDetail[];
        } catch {
          details = [];
        }
        if (details.length) {
          setError(
            details
              .map(
                (d) =>
                  `${d.sku} requested ${d.requested}, only ${d.available} left. Quantity was reduced.`,
              )
              .join(" "),
          );
          for (const d of details) {
            const available = Number(d.available);
            if (!d.sku || !Number.isFinite(available)) {
              continue;
            }
            await api.put("/api/cart/items", { sku: d.sku, qty: Math.max(0, available) });
          }
        } else {
          setError("Not enough stock. Lower the quantities and try again.");
        }
        await refresh();
      }
      if (!cancelled) {
        setBooting(false);
      }
    }
    void boot();
    return () => {
      cancelled = true;
    };
  }, [refresh]);

  function change(sku: string, qty: number, stock: number) {
    void setQty(sku, Math.min(Math.max(0, qty), stock)).catch((err: unknown) => {
      setError(err instanceof Error ? err.message : "Could not update quantity");
    });
  }

  const loading = booting || (status === "loading" && items.length === 0);
  const cartTotal = items.reduce((sum, line) => sum + Number(line.price) * line.qty, 0);

  return (
    <Layout title="Cart">
      <div className="sheet">
        <StatusBanner error={error} />
        {loading ? <p className="copy-center">Loading…</p> : null}
        {!loading && items.length === 0 ? (
          <p className="muted copy-center">
            Cart is empty. <Link to="/">Shop</Link>
          </p>
        ) : null}
        {!loading && items.length > 0 ? (
          <>
            <table className="data cart-table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Description</th>
                  <th className="qty-cell">Quantity</th>
                  <th>Available</th>
                  <th className="num">Unit price</th>
                  <th className="num">Total</th>
                </tr>
              </thead>
              <tbody>
                {items.map((line) => {
                  const href = productPath(line.sku);
                  return (
                    <tr key={line.sku}>
                      <td>
                        <div className="cart-product">
                          <Link className="thumb-link" to={href} aria-label={line.name}>
                            <ProductImage className="thumb" sku={line.sku} name={line.name} />
                          </Link>
                          <Link className="cart-product-name" to={href}>
                            {line.name}
                          </Link>
                        </div>
                      </td>
                      <td>
                        <p className="cart-desc">{line.description || "—"}</p>
                      </td>
                      <td className="qty-cell">
                        <CartLineQty
                          name={line.name}
                          qty={line.qty}
                          stock={line.stock}
                          onQty={(qty) => change(line.sku, qty, line.stock)}
                          onRemove={() => change(line.sku, 0, line.stock)}
                        />
                      </td>
                      <td>{line.stock} available</td>
                      <td className="num">{money(line.price)}</td>
                      <td className="num">{money(Number(line.price) * line.qty)}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            <div className="pay-dock">
              <p className="cart-total">
                <span>Cart total</span>
                <strong>{money(cartTotal)}</strong>
              </p>
              <Link className="btn btn-wide" to="/checkout">
                Checkout
              </Link>
            </div>
          </>
        ) : null}
      </div>
    </Layout>
  );
}
