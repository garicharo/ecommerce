import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import { ListStatus } from "../components/StatusBanner";
import { money, formatWhen, type PageResponse, type Product } from "../types";

export function AdminProducts() {
  const [items, setItems] = useState<Product[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [error, setError] = useState("");
  const [status, setStatus] = useState<"loading" | "ok">("loading");
  const size = 20;

  useEffect(() => {
    setStatus("loading");
    api
      .get<PageResponse<Product>>(`/api/products?size=${size}&page=${page}&sort=name_asc`, {
        redirectOn401: false,
      })
      .then((body) => {
        setItems(body.items ?? []);
        setTotal(body.total ?? 0);
        setStatus("ok");
      })
      .catch((err: unknown) => {
        setError(err instanceof Error ? err.message : "Could not load products");
        setStatus("ok");
      });
  }, [page]);

  const hasPrev = page > 0;
  const hasNext = (page + 1) * size < total;

  return (
    <>
      <ListStatus
        loading={status === "loading"}
        error={error}
        empty={status === "ok" && items.length === 0}
        emptyCopy={
          <>
            No products yet. <Link to="/admin/imports">Import a CSV</Link>
          </>
        }
      >
        <div className="catalog-table-wrap">
          <table className="catalog">
            <thead>
              <tr>
                <th>SKU</th>
                <th>Name</th>
                <th>Description</th>
                <th>Category</th>
                <th className="num">Price</th>
                <th className="num">Stock</th>
                <th className="num">Weight (kg)</th>
                <th>Origin</th>
                <th>Updated</th>
                <th className="num">Version</th>
              </tr>
            </thead>
            <tbody>
              {items.map((p) => (
                <tr key={p.sku}>
                  <td>
                    <Link to={`/admin/products/${encodeURIComponent(p.sku)}`}>{p.sku}</Link>
                  </td>
                  <td>{p.name}</td>
                  <td className="desc-cell" title={p.description}>
                    {p.description}
                  </td>
                  <td>{p.category}</td>
                  <td className="num">{money(p.price)}</td>
                  <td className="num">{p.stock}</td>
                  <td className="num">{p.weightKg}</td>
                  <td>{p.origin}</td>
                  <td className="num">{p.updatedAt ? formatWhen(p.updatedAt) : "—"}</td>
                  <td className="num">{p.version}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {hasPrev || hasNext ? (
          <div className="pager pager-compact">
            <div className="pager-pair">
              {hasPrev ? (
                <button className="btn secondary pager-prev" type="button" onClick={() => setPage((n) => n - 1)}>
                  Previous
                </button>
              ) : null}
              {hasNext ? (
                <button className="btn secondary pager-next" type="button" onClick={() => setPage((n) => n + 1)}>
                  Next
                </button>
              ) : null}
            </div>
          </div>
        ) : null}
      </ListStatus>
    </>
  );
}
