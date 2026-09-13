import { type SubmitEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { catalogProductPath } from "../catalogQuery";
import { money, type Product } from "../types";
import { useAddToCart } from "../useAddToCart";
import { ProductImage } from "./ProductImage";
import { QtyPicker } from "./QtyPicker";

type Props = {
  product: Product;
  onAdded: (message: string) => void;
  onError: (message: string) => void;
};

export function ProductCard({ product, onAdded, onError }: Props) {
  const [params] = useSearchParams();
  const href = catalogProductPath(product.sku, params);
  const { qty, setQty, busy, line, remaining, out, full, add } = useAddToCart(product);

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      const message = await add();
      if (message) {
        onAdded(message);
      }
    } catch (err) {
      onError(err instanceof Error ? err.message : "Could not add to cart");
    }
  }

  return (
    <article className="product-card">
      <Link className="product-card-media" to={href}>
        <ProductImage sku={product.sku} name={product.name} category={product.category} />
      </Link>
      <div className="product-card-body">
        <h2>
          <Link className="product-card-title" to={href}>
            {product.name}
          </Link>
        </h2>
        <p className="product-card-meta">{money(product.price)}</p>
        <p className="in-cart">{line ? `In cart · ${line.qty}` : "\u00a0"}</p>
        <form className="product-card-add" onSubmit={(event) => void submit(event)}>
          <QtyPicker
            compact
            stock={product.stock}
            maxAdd={remaining}
            value={qty}
            onChange={setQty}
            disabled={out || full}
          />
          <button className="btn" type="submit" disabled={out || full || busy}>
            {out ? "Out of stock" : full ? "In cart" : busy ? "Adding…" : "Add to cart"}
          </button>
        </form>
      </div>
    </article>
  );
}
