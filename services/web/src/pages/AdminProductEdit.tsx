import { type SubmitEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { HttpError, api } from "../api";
import { StatusBanner } from "../components/StatusBanner";
import { type Product } from "../types";

export function AdminProductEdit() {
  const { sku = "" } = useParams();
  const decoded = decodeURIComponent(sku);
  const navigate = useNavigate();
  const [product, setProduct] = useState<Product | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api
      .get<Product>(`/api/products/${encodeURIComponent(decoded)}`, { redirectOn401: false })
      .then(setProduct)
      .catch((err: unknown) => setError(err instanceof Error ? err.message : "Not found"));
  }, [decoded]);

  async function save(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!product) {
      return;
    }
    const fd = new FormData(event.currentTarget);
    setBusy(true);
    setError("");
    try {
      const updated = await api.put<Product>(`/api/admin/products/${encodeURIComponent(decoded)}`, {
        name: fd.get("name"),
        description: fd.get("description"),
        category: fd.get("category"),
        price: fd.get("price"),
        stock: Number(fd.get("stock")),
        weightKg: fd.get("weightKg"),
        version: product.version,
      });
      setProduct(updated);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Update failed");
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!window.confirm(`Delete ${decoded}? If it appears on an order, set stock to 0 instead.`)) {
      return;
    }
    setBusy(true);
    try {
      await api.del(`/api/admin/products/${encodeURIComponent(decoded)}`);
      navigate("/admin/products");
    } catch (err: unknown) {
      setError(err instanceof HttpError ? err.message : "Delete failed");
      setBusy(false);
    }
  }

  if (!product && !error) {
    return <p>Loading…</p>;
  }
  if (!product) {
    return <StatusBanner error={error} />;
  }

  return (
    <>
      <p>
        <Link to="/admin/products">← Products</Link>
      </p>
      <StatusBanner error={error} />
      <form className="card narrow" onSubmit={(e) => void save(e)}>
        <p className="muted">
          {product.sku} · origin {product.origin} · version {product.version}
        </p>
        <label className="field">
          Name <input name="name" required defaultValue={product.name} />
        </label>
        <label className="field">
          Description <input name="description" required defaultValue={product.description} />
        </label>
        <label className="field">
          Category <input name="category" required defaultValue={product.category} />
        </label>
        <label className="field">
          Price <input name="price" required defaultValue={String(product.price)} />
        </label>
        <label className="field">
          Stock <input name="stock" type="number" min={0} required defaultValue={product.stock} />
        </label>
        <label className="field">
          Weight kg <input name="weightKg" required defaultValue={String(product.weightKg)} />
        </label>
        <p>
          <button className="btn" type="submit" disabled={busy}>
            {busy ? "Saving…" : "Save"}
          </button>{" "}
          <button className="btn secondary" type="button" disabled={busy} onClick={() => void remove()}>
            Delete
          </button>
        </p>
      </form>
    </>
  );
}
