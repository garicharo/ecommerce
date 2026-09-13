import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "./AuthProvider";
import { cartLine, useCart } from "./cart";
import { savePendingAdd } from "./pendingAdd";
import { type Product } from "./types";

export function useAddToCart(product: Product | null) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { items, addMore } = useCart();
  const line = product ? cartLine(items, product.sku) : undefined;
  const remaining = product ? Math.max(0, product.stock - (line?.qty ?? 0)) : 0;
  const out = product?.stock === 0;
  const full = Boolean(product && !out && remaining === 0);
  const [qty, setQty] = useState(product && product.stock > 0 ? 1 : 0);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!product) {
      return;
    }
    setQty(product.stock > 0 ? 1 : 0);
  }, [product]);

  useEffect(() => {
    if (remaining > 0 && qty > remaining) {
      setQty(remaining);
    }
  }, [remaining, qty]);

  async function add(): Promise<string | null> {
    if (!product) {
      return null;
    }
    if (!user) {
      savePendingAdd(product.sku, qty);
      navigate("/login");
      return null;
    }
    if (full) {
      return `All ${product.stock} are already in your cart`;
    }
    setBusy(true);
    try {
      const result = await addMore(product.sku, qty, product.stock);
      setQty(result.remaining > 0 ? 1 : 0);
      return `Added ${product.name} to cart`;
    } finally {
      setBusy(false);
    }
  }

  return { qty, setQty, busy, line, remaining, out, full, add };
}
