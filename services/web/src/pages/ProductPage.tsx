import { type SubmitEvent, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Layout } from "../Layout";
import { ProductImage } from "../components/ProductImage";
import { QtyPicker } from "../components/QtyPicker";
import { StatusBanner } from "../components/StatusBanner";
import { api } from "../api";
import { money, type Product } from "../types";
import { useAddToCart } from "../useAddToCart";
import { useToast } from "../toast";

export function ProductPage() {
  const { sku = "" } = useParams();
  const decoded = decodeURIComponent(sku);
  const [product, setProduct] = useState<Product | null>(null);
  const [status, setStatus] = useState<"loading" | "ok" | "error">("loading");
  const [error, setError] = useState("");
  const { show } = useToast();
  const { qty, setQty, busy, line, remaining, out, full, add } = useAddToCart(product);

  useEffect(() => {
    setStatus("loading");
    api
      .get<Product>(`/api/products/${encodeURIComponent(decoded)}`, {
        auth: false,
        redirectOn401: false,
      })
      .then((body) => {
        setProduct(body);
        setStatus("ok");
      })
      .catch((err: unknown) => {
        setStatus("error");
        setError(err instanceof Error ? err.message : "Product not found");
      });
  }, [decoded]);

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    try {
      const message = await add();
      if (message) {
        show(message);
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Could not add to cart");
    }
  }

  return (
    <Layout>
      {status === "loading" ? <p>Loading…</p> : null}
      <StatusBanner error={error} />
      {status === "ok" && product ? (
        <div className="hero">
          <ProductImage sku={product.sku} name={product.name} category={product.category} />
          <div className="hero-copy">
            <h1>{product.name}</h1>
            <p className="muted">
              {money(product.price)} · {product.weightKg} kg · {product.category}
            </p>
            <p className="blurb">{product.description}</p>
            <form className="card buy-box" onSubmit={(event) => void submit(event)}>
              <QtyPicker
                stock={product.stock}
                maxAdd={remaining}
                value={qty}
                onChange={setQty}
                disabled={out || full}
              />
              {out ? null : (
                <button className="btn" type="submit" disabled={full || busy}>
                  {full ? "In cart" : busy ? "Adding…" : "Add to cart"}
                </button>
              )}
            </form>
            {line ? <p className="in-cart">In cart · {line.qty}</p> : null}
          </div>
        </div>
      ) : null}
    </Layout>
  );
}
