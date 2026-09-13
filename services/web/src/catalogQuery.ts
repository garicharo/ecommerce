export const DEFAULT_SORT = "name_asc";

export type CatalogQuery = {
  q: string;
  category: string;
  sort: string;
  page: number;
};

export function readCatalogQuery(params: URLSearchParams): CatalogQuery {
  const qRaw = (params.get("q") ?? "").trim();
  return {
    q: qRaw.length >= 2 ? qRaw : "",
    category: params.get("category") ?? "",
    sort: params.get("sort") || DEFAULT_SORT,
    page: Math.max(0, Number(params.get("page") ?? "0") || 0),
  };
}

export function writeCatalogQuery(
  params: URLSearchParams,
  patch: Partial<CatalogQuery>,
): URLSearchParams {
  const current = readCatalogQuery(params);
  const next = { ...current, ...patch };
  const out = new URLSearchParams();
  if (next.q) {
    out.set("q", next.q);
  }
  if (next.category) {
    out.set("category", next.category);
  }
  if (next.sort && next.sort !== DEFAULT_SORT) {
    out.set("sort", next.sort);
  }
  if (next.page > 0) {
    out.set("page", String(next.page));
  }
  return out;
}

export function catalogHome(params: URLSearchParams, patch: Partial<CatalogQuery> = {}): string {
  const qs = writeCatalogQuery(params, patch).toString();
  return qs ? `/?${qs}` : "/";
}

export function catalogProductPath(sku: string, params: URLSearchParams): string {
  const qs = writeCatalogQuery(params, { page: 0 }).toString();
  const path = `/products/${encodeURIComponent(sku)}`;
  return qs ? `${path}?${qs}` : path;
}
