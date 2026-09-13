import { useEffect, type ReactNode } from "react";
import { Navigate, NavLink, Outlet, useLocation } from "react-router-dom";
import { rememberAdminTab } from "./adminTab";
import { useAuth } from "./AuthProvider";
import { useCart } from "./cart";
import { Layout } from "./Layout";

function adminHeading(pathname: string): string {
  if (pathname === "/admin/products/new") {
    return "Add product";
  }
  if (pathname.startsWith("/admin/products")) {
    return "Catalog";
  }
  if (pathname.startsWith("/admin/imports")) {
    return "CSV import";
  }
  if (pathname.startsWith("/admin/orders")) {
    return "Orders";
  }
  return "CSV import";
}

function adminInset(pathname: string): string {
  return pathname.startsWith("/admin/imports") ? "inset-15" : "inset-10";
}

export function RequireAuth({ children }: { children: ReactNode }) {
  const { user, status } = useAuth();
  if (status === "loading") {
    return (
      <Layout>
        <p className="copy-center">Loading…</p>
      </Layout>
    );
  }
  if (!user) {
    const next = encodeURIComponent(`${window.location.pathname}${window.location.search}`);
    return <Navigate to={`/login?next=${next}`} replace />;
  }
  return children;
}

export function RequireCart({ children }: { children: ReactNode }) {
  const { items, status } = useCart();
  const waiting = status === "idle" || (status === "loading" && items.length === 0);
  if (waiting) {
    return (
      <Layout>
        <p className="copy-center">Loading…</p>
      </Layout>
    );
  }
  if (items.length === 0) {
    return <Navigate to="/" replace />;
  }
  return children;
}

export function AdminLayout() {
  const { user, status } = useAuth();
  const { pathname } = useLocation();

  useEffect(() => {
    rememberAdminTab(pathname);
  }, [pathname]);

  if (status === "loading") {
    return (
      <Layout>
        <p className="copy-center">Loading…</p>
      </Layout>
    );
  }
  if (!user) {
    return <Navigate to="/login?next=/admin" replace />;
  }
  if (user.role !== "ADMIN") {
    return <Navigate to="/" replace />;
  }

  return (
    <Layout>
      <div className="admin-page">
        <h1>{adminHeading(pathname)}</h1>
        <nav className="tabs" aria-label="Admin">
          <NavLink to="/admin/imports">CSV import</NavLink>
          <NavLink to="/admin/products" end>
            Catalog
          </NavLink>
          <NavLink to="/admin/products/new">Add product</NavLink>
          <NavLink to="/admin/orders">Orders</NavLink>
        </nav>
        <div className={adminInset(pathname)}>
          <Outlet />
        </div>
      </div>
    </Layout>
  );
}
