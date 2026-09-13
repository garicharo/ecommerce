import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Layout } from "../Layout";
import { ListStatus, StatusBanner } from "../components/StatusBanner";
import { ProductCard } from "../components/ProductCard";
import { api } from "../api";
import { readCatalogQuery, writeCatalogQuery } from "../catalogQuery";
import { useToast } from "../toast";
import { type PageResponse, type Product } from "../types";

export function Shop() {
  const [params, setParams] = useSearchParams();
  const { q, category, sort, page } = readCatalogQuery(params);
  const [data, setData] = useState<PageResponse<Product> | null>(null);
  const [status, setStatus] = useState<"loading" | "ok" | "error">("loading");
  const [error, setError] = useState("");
  const { show } = useToast();

  useEffect(() => {
    const query = new URLSearchParams();
    if (q) {
      query.set("q", q);
    }
    if (category) {
      query.set("category", category);
    }
    query.set("sort", sort);
    query.set("page", String(page));
    query.set("size", "24");
    setStatus("loading");
    setError("");
    api
      .get<PageResponse<Product>>(`/api/products?${query}`, { auth: false, redirectOn401: false })
      .then((body) => {
        setData(body);
        setStatus("ok");
      })
      .catch((err: unknown) => {
        setStatus("error");
        setError(err instanceof Error ? err.message : "Could not load catalog");
      });
  }, [q, category, sort, page]);

  const items = data?.items ?? [];
  const total = data?.total ?? 0;
  const size = data?.size ?? 24;
  const hasPrev = page > 0;
  const hasNext = (page + 1) * size < total;
  const emptyCopy =
    q || category ? "No products match those filters." : "No products yet.";

  function setPage(nextPage: number) {
    setParams(writeCatalogQuery(params, { page: nextPage }), { replace: true });
  }

  return (
    <Layout>
      <StatusBanner error={error} />
      <ListStatus
        loading={status === "loading"}
        empty={status === "ok" && items.length === 0}
        emptyCopy={emptyCopy}
      >
        <div className="product-grid">
          {items.map((product) => (
            <ProductCard
              key={product.sku}
              product={product}
              onAdded={show}
              onError={(message) => {
                setError(message);
              }}
            />
          ))}
        </div>
      </ListStatus>
      {hasPrev || hasNext ? (
        <div className="pager">
          <div className="pager-pair">
            {hasPrev ? (
              <button className="btn secondary pager-prev" type="button" onClick={() => setPage(page - 1)}>
                Previous
              </button>
            ) : null}
            {hasNext ? (
              <button className="btn secondary pager-next" type="button" onClick={() => setPage(page + 1)}>
                Next
              </button>
            ) : null}
          </div>
        </div>
      ) : null}
    </Layout>
  );
}
