const KEY = "gila.adminTab";

const TABS = ["/admin/imports", "/admin/products", "/admin/products/new", "/admin/orders"] as const;

export function adminTabFromPath(pathname: string): (typeof TABS)[number] {
  if (pathname === "/admin/products/new") {
    return "/admin/products/new";
  }
  if (pathname.startsWith("/admin/products")) {
    return "/admin/products";
  }
  if (pathname.startsWith("/admin/orders")) {
    return "/admin/orders";
  }
  return "/admin/imports";
}

export function rememberAdminTab(pathname: string): void {
  if (!pathname.startsWith("/admin")) {
    return;
  }
  try {
    localStorage.setItem(KEY, adminTabFromPath(pathname));
  } catch {
    /* ignore quota / private mode */
  }
}

export function lastAdminTab(): string {
  try {
    const saved = localStorage.getItem(KEY);
    return TABS.find((tab) => tab === saved) ?? "/admin/imports";
  } catch {
    return "/admin/imports";
  }
}
