import { Link } from "react-router-dom";
import { formatWhen, money, productPath, type Order } from "../types";
import { ProductImage } from "./ProductImage";

type Props = {
  order: Order;
  showId?: boolean;
};

const PREVIEW_LINES = 3;

export function OrderCard({ order, showId = false }: Props) {
  const lines = order.items ?? [];
  const extra = Math.max(0, lines.length - PREVIEW_LINES);
  const shown = extra > 0 ? lines.slice(0, PREVIEW_LINES) : lines;
  return (
    <article className="card order-card">
      <header className="order-head">
        <div>
          <p className="order-status">{order.status}</p>
          <p className="muted order-when">{formatWhen(order.createdAt)}</p>
          {showId ? <p className="muted order-ref">Order {order.id.slice(0, 8)}</p> : null}
        </div>
        <strong className="order-total">{money(order.total)}</strong>
      </header>
      <ul className="confirm-lines">
        {shown.map((line) => {
          const href = productPath(line.sku);
          return (
            <li key={`${order.id}-${line.sku}`} className="confirm-line">
              <Link className="thumb-link" to={href} aria-label={line.name}>
                <ProductImage className="thumb" sku={line.sku} name={line.name} />
              </Link>
              <div className="confirm-copy">
                <Link className="cart-product-name" to={href}>
                  {line.name}
                </Link>
                <p className="muted">
                  {line.qty} × {money(line.unitPrice)}
                </p>
              </div>
              <span className="num">{money(line.lineTotal)}</span>
            </li>
          );
        })}
      </ul>
      {extra > 0 ? <p className="muted order-more">{extra} more</p> : null}
    </article>
  );
}
