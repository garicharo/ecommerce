type Props = {
  name: string;
  qty: number;
  stock: number;
  busy?: boolean;
  onQty: (qty: number) => void;
  onRemove: () => void;
};

export function CartLineQty({ name, qty, stock, busy, onQty, onRemove }: Props) {
  const cap = Math.max(0, stock);
  const shown = Math.min(qty, cap);

  return (
    <div className="cart-qty">
      <div className="qty-step">
        <button
          type="button"
          aria-label={`Decrease ${name}`}
          disabled={busy || shown <= 1}
          onClick={() => onQty(shown - 1)}
        >
          −
        </button>
        <span aria-live="polite">{shown}</span>
        <button
          type="button"
          aria-label={`Increase ${name}`}
          disabled={busy || shown >= cap}
          onClick={() => onQty(shown + 1)}
        >
          +
        </button>
      </div>
      <button
        type="button"
        className="icon-btn"
        aria-label={`Remove ${name} from cart`}
        disabled={busy}
        onClick={onRemove}
      >
        <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
          <path
            fill="currentColor"
            d="M9 3h6l1 2h5v2H3V5h5l1-2zm1 6h2v10h-2V9zm4 0h2v10h-2V9zM7 9h2v10H7V9z"
          />
        </svg>
      </button>
    </div>
  );
}
