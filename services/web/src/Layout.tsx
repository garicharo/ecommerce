import { type ReactNode } from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "./AuthProvider";
import { ShopSearch } from "./components/ShopSearch";
import { shortName } from "./auth";
import { lastAdminTab } from "./adminTab";
import { CartRail, useCart, useShowCartRail } from "./cart";

type Props = {
  children: ReactNode;
  title?: string;
};

function CartGlyph() {
  return (
    <svg className="nav-cart-icon" viewBox="0 0 24 24" aria-hidden="true">
      <path
        fill="currentColor"
        d="M7 18a2 2 0 1 0 0 4 2 2 0 0 0 0-4Zm10 0a2 2 0 1 0 0 4 2 2 0 0 0 0-4ZM3.15 4h1.72l.32 2H20a1 1 0 0 1 .98 1.2l-1.4 7A2 2 0 0 1 17.62 16H8.28L7.6 19H19v2H6.55a1.5 1.5 0 0 1-1.47-1.8L6.7 8.2 6.1 6H3.15V4Z"
      />
    </svg>
  );
}

export function Layout({ children, title }: Props) {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const { user, logout } = useAuth();
  const { count, status: cartStatus, refresh } = useCart();
  const showRail = useShowCartRail();
  const cartKnownEmpty = cartStatus === "ok" && count === 0;

  async function onLogout() {
    await logout();
    await refresh();
    navigate("/login");
  }

  const cartLabel = count ? `Cart, ${count} items` : "Cart is empty";

  return (
    <div className="shell">
      <header className={`topbar${showRail ? " has-rail" : ""}`}>
        <Link className="brand" to="/">
          Gila Store
        </Link>
        <ShopSearch />
        <div className="top-tools">
          <nav className="nav">
            {user ? (
              <>
                {cartKnownEmpty ? (
                  <button type="button" className="nav-cart is-off" disabled aria-label={cartLabel} title={cartLabel}>
                    <CartGlyph />
                  </button>
                ) : (
                  <NavLink to="/cart" className="nav-cart" aria-label={cartLabel} title={cartLabel}>
                    <CartGlyph />
                    {count > 0 ? <span className="nav-cart-count">{count > 99 ? "99+" : count}</span> : null}
                  </NavLink>
                )}
                <NavLink to="/orders">Orders</NavLink>
                {user.role === "ADMIN" ? <NavLink to={lastAdminTab()}>Admin</NavLink> : null}
                <button
                  className="nav-session"
                  type="button"
                  onClick={() => void onLogout()}
                  title={user.displayName ? `${user.displayName} — Log out` : "Log out"}
                >
                  {user.displayName ? <span className="who">{shortName(user.displayName)}</span> : null}
                  {user.displayName ? <span aria-hidden="true">·</span> : null}
                  Log out
                </button>
              </>
            ) : pathname.startsWith("/login") ? null : (
              <NavLink to="/login">Log in</NavLink>
            )}
          </nav>
        </div>
      </header>
      <div className={`shop-frame${showRail ? " has-rail" : ""}`}>
        <div className="shop-main">
          {title ? <h1>{title}</h1> : null}
          {children}
        </div>
        {showRail ? <CartRail /> : null}
      </div>
    </div>
  );
}
