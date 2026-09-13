import { useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Layout } from "../Layout";
import { ProductImage } from "../components/ProductImage";
import { StatusBanner } from "../components/StatusBanner";
import { HttpError, api } from "../api";
import { useCart } from "../cart";
import { money, productPath, type CheckoutResult } from "../types";

export function CheckoutPage() {
  const navigate = useNavigate();
  const keyRef = useRef(crypto.randomUUID());
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const { items, status, refresh } = useCart();
  const loading = status === "idle" || (status === "loading" && items.length === 0);
  const total = items.reduce((sum, line) => sum + Number(line.price) * line.qty, 0);

  async function buy() {
    setBusy(true);
    setError("");
    try {
      await api.post<CheckoutResult>("/api/checkout", undefined, {
        idempotencyKey: keyRef.current,
      });
      await refresh();
      navigate("/orders");
    } catch (err: unknown) {
      if (err instanceof HttpError && err.status === 409 && err.body.code === "INSUFFICIENT_STOCK") {
        sessionStorage.setItem("stockError", JSON.stringify(err.body.details ?? []));
        navigate("/cart");
        return;
      }
      setError(err instanceof Error ? err.message : "Checkout failed");
      setBusy(false);
    }
  }

  return (
    <Layout>
      <div className="sheet review">
        <h1>Review and confirm</h1>
        <StatusBanner error={error} />
        {loading ? <p className="copy-center">Loading…</p> : null}
        {!loading && items.length === 0 ? (
          <p className="copy-center">
            Cart is empty. <Link to="/">Shop</Link>
          </p>
        ) : null}
        {!loading && items.length > 0 ? (
          <>
            <div className="card confirm">
              <ul className="confirm-lines">
                {items.map((line) => {
                  const href = productPath(line.sku);
                  return (
                    <li key={line.sku} className="confirm-line">
                      <Link className="thumb-link" to={href} aria-label={line.name}>
                        <ProductImage className="thumb" sku={line.sku} name={line.name} />
                      </Link>
                      <div className="confirm-copy">
                        <Link className="cart-product-name" to={href}>
                          {line.name}
                        </Link>
                        {line.description ? <p className="muted cart-desc">{line.description}</p> : null}
                        <p className="muted">
                          {line.qty} × {money(line.price)}
                        </p>
                      </div>
                      <span className="num">{money(Number(line.price) * line.qty)}</span>
                    </li>
                  );
                })}
              </ul>
            </div>
            <div className="pay-dock">
              <p className="cart-total">
                <span>Total</span>
                <strong>{money(total)}</strong>
              </p>
              <button className="btn btn-pay" type="button" disabled={busy} onClick={() => void buy()}>
                {busy ? "Buying…" : "Buy"}
              </button>
            </div>
          </>
        ) : null}
      </div>
    </Layout>
  );
}
