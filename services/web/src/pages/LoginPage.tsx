import { type SubmitEvent, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../AuthProvider";
import { Layout } from "../Layout";
import { StatusBanner } from "../components/StatusBanner";
import { HttpError } from "../api";
import { useCart } from "../cart";
import { lastAdminTab } from "../adminTab";
import { applyPendingAdd, peekPendingAdd } from "../pendingAdd";

export function LoginPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const { login } = useAuth();
  const { addMore, refresh } = useCart();
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const pending = peekPendingAdd();

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const fd = new FormData(event.currentTarget);
    const email = String(fd.get("email") ?? "");
    const password = String(fd.get("password") ?? "");
    setBusy(true);
    setError("");
    try {
      const me = await login(email, password);
      const fromCart = await applyPendingAdd(addMore, refresh);
      if (fromCart) {
        navigate(fromCart);
        return;
      }
      const next = params.get("next");
      const dest = next && next.startsWith("/") && !next.startsWith("/login") ? next : "";
      if (me.role === "ADMIN" && (!dest || dest === "/" || dest === "/admin")) {
        navigate(lastAdminTab());
      } else {
        navigate(dest || "/");
      }
    } catch (err: unknown) {
      setError(err instanceof HttpError && err.status === 401 ? "Invalid credentials." : "Could not log in.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Layout>
      <div className="auth-panel">
        <h1>Log in</h1>
        <p className="muted">
          {pending
            ? "Log in to add that item to your cart. You will come back to the shop."
            : "Demo admin email admin@shop.local, password admin1234"}
        </p>
        <form className="card" onSubmit={(event) => void submit(event)}>
          <label className="field">
            Email
            <input name="email" type="email" required autoComplete="username" />
          </label>
          <label className="field">
            Password
            <input name="password" type="password" required autoComplete="current-password" />
          </label>
          <StatusBanner error={error} />
          <button className="btn btn-auth" type="submit" disabled={busy}>
            {busy ? "Logging in…" : "Log in"}
          </button>
        </form>
        <p>
          <Link to="/signup">Sign up</Link>
        </p>
      </div>
    </Layout>
  );
}
