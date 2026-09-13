import { type SubmitEvent, useState } from "react";
import { Link } from "react-router-dom";
import { StatusBanner } from "../components/StatusBanner";
import { api } from "../api";
import { useToast } from "../toast";

export function AdminHome() {
  const { show } = useToast();
  const [error, setError] = useState("");

  async function create(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const fd = new FormData(form);
    const name = String(fd.get("name") ?? "").trim();
    const stock = Number(fd.get("stock"));
    setError("");
    try {
      await api.post("/api/admin/products", {
        sku: fd.get("sku"),
        name,
        description: fd.get("description"),
        category: fd.get("category"),
        price: fd.get("price"),
        stock,
        weightKg: fd.get("weightKg"),
      });
      form.reset();
      show(
        stock === 0
          ? `${name} created with 0 stock. It stays in the catalog as out of stock.`
          : `${name} created.`,
      );
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Create failed");
    }
  }

  return (
    <div className="admin-create">
      <form className="card product-form" onSubmit={(event) => void create(event)}>
        <StatusBanner error={error} />
        <div className="product-form-fields">
          <label className="field">
            SKU <input name="sku" required />
          </label>
          <label className="field">
            Name <input name="name" required />
          </label>
          <label className="field">
            Description <input name="description" required />
          </label>
          <label className="field">
            Category <input name="category" required />
          </label>
          <label className="field">
            Price <input name="price" required defaultValue="10.00" />
          </label>
          <label className="field">
            Stock
            <input name="stock" type="number" min={0} required defaultValue={1} />
          </label>
          <label className="field">
            Weight kg <input name="weightKg" required defaultValue="0.500" />
          </label>
        </div>
        <button className="btn btn-create" type="submit">
          Create
        </button>
      </form>
      <p className="admin-csv-link">
        Many SKUs? <Link to="/admin/imports">Import a CSV</Link>
      </p>
    </div>
  );
}
