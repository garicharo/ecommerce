import { type SubmitEvent, useCallback, useEffect, useState } from "react";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../api";
import { catalogHome, readCatalogQuery, writeCatalogQuery } from "../catalogQuery";
import { useDebouncedValue } from "../hooks";
import { type CategoryList } from "../types";

function useCatalogControls() {
  const [params, setParams] = useSearchParams();
  const location = useLocation();
  const navigate = useNavigate();
  const catalog = readCatalogQuery(params);
  const hideCategory =
    location.pathname.startsWith("/admin") ||
    location.pathname.startsWith("/login") ||
    location.pathname.startsWith("/signup");

  const apply = useCallback(
    (patch: Partial<typeof catalog>) => {
      const next = writeCatalogQuery(params, { ...patch, page: 0 });
      if (location.pathname === "/") {
        setParams(next, { replace: true });
        return;
      }
      navigate(catalogHome(params, { ...patch, page: 0 }));
    },
    [location.pathname, navigate, params, setParams],
  );

  return { catalog, apply, hideCategory, location };
}

export function ShopSearch() {
  const [params] = useSearchParams();
  const { catalog, apply, hideCategory, location } = useCatalogControls();
  const qParam = params.get("q") ?? "";
  const [draft, setDraft] = useState(qParam);
  const [categories, setCategories] = useState<string[]>([]);
  const debounced = useDebouncedValue(draft, 300);

  useEffect(() => {
    setDraft((current) => {
      if (!qParam && current.trim().length > 0 && current.trim().length < 2) {
        return current;
      }
      return qParam;
    });
  }, [qParam]);

  useEffect(() => {
    const q = debounced.trim().length >= 2 ? debounced.trim() : "";
    if (location.pathname.startsWith("/products/")) {
      return;
    }
    if (q === catalog.q) {
      return;
    }
    apply({ q });
  }, [apply, catalog.q, debounced, location.pathname]);

  useEffect(() => {
    if (hideCategory) {
      return;
    }
    api
      .get<CategoryList>("/api/products/categories", { auth: false, redirectOn401: false })
      .then((body) => setCategories(body.items ?? []))
      .catch(() => setCategories([]));
  }, [hideCategory]);

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const q = draft.trim().length >= 2 ? draft.trim() : "";
    apply({ q });
  }

  return (
    <form className={`top-search${hideCategory ? "" : " with-cat"}`} role="search" onSubmit={submit}>
      {hideCategory ? null : (
        <label className="search-cat">
          <span className="sr-only">Category</span>
          <select
            value={catalog.category}
            onChange={(event) => apply({ category: event.target.value })}
            title="Category"
          >
            <option value="">All categories</option>
            {categories.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </select>
        </label>
      )}
      <label className="search-q">
        <span className="sr-only">Search</span>
        <input
          type="search"
          autoComplete="off"
          placeholder="Search products"
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
        />
      </label>
    </form>
  );
}
