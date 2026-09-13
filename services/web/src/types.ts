export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  total: number;
};

export type Product = {
  id: string;
  sku: string;
  name: string;
  description: string;
  category: string;
  price: number | string;
  stock: number;
  weightKg: number | string;
  origin: string;
  version: number;
  createdAt?: string;
  updatedAt?: string;
};

export type CartItem = {
  sku: string;
  qty: number;
  name: string;
  description?: string;
  price: number | string;
  stock: number;
};

export type Cart = {
  items: CartItem[];
};

export type OrderLine = {
  sku: string;
  name: string;
  qty: number;
  unitPrice: number | string;
  lineTotal: number | string;
};

export type Order = {
  id: string;
  status: string;
  total: number | string;
  createdAt: string;
  items: OrderLine[];
};

export type ApiError = {
  code: string;
  message: string;
  details: Array<{ sku?: string; requested?: number; available?: number }>;
};

export type CheckoutResult = {
  id: string;
  status: string;
};

export type CategoryList = {
  items: string[];
};

export type ImportJob = {
  jobId: string;
  filename: string;
  status: string;
  totalRows: number;
  inserted: number;
  updated: number;
  failed: number;
  skipped: number;
  headline: string;
  errorsGrouped: Array<{ code: string; count: number }>;
  createdAt?: string;
  finishedAt?: string;
};

export type ImportRow = {
  lineNumber: number;
  sku: string | null;
  outcome: string;
  code: string | null;
  message: string | null;
};

export { initials, productImage, relatedProductImage } from "./productPhotos";

export function formatWhen(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return iso;
  }
  return new Intl.DateTimeFormat("en-US", { dateStyle: "medium", timeStyle: "short" }).format(date);
}

export function formatWhenDay(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return iso;
  }
  return new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric" }).format(date);
}

export function jobStatusLabel(status: string): string {
  if (status === "FAILED_HEADER") {
    return "Header";
  }
  if (status === "COMPLETED") {
    return "Completed";
  }
  return status.charAt(0) + status.slice(1).toLowerCase();
}

export function money(value: number | string): string {
  const n = typeof value === "number" ? value : Number(value);
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(
    Number.isFinite(n) ? n : 0,
  );
}

export function productPath(sku: string): string {
  return `/products/${encodeURIComponent(sku)}`;
}
