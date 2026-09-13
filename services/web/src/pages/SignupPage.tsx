import { type SubmitEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthProvider";
import { Layout } from "../Layout";
import { StatusBanner } from "../components/StatusBanner";
import { HttpError } from "../api";
import { useCart } from "../cart";
import { applyPendingAdd } from "../pendingAdd";

export function SignupPage() {
  const navigate = useNavigate();
  const { signup } = useAuth();
  const { addMore, refresh } = useCart();
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const fd = new FormData(event.currentTarget);
    const email = String(fd.get("email") ?? "").trim();
    const displayName = String(fd.get("displayName") ?? "").trim();
    const password = String(fd.get("password") ?? "");
    setBusy(true);
    setError("");
    try {
      await signup(email, password, displayName);
      const fromCart = await applyPendingAdd(addMore, refresh);
      navigate(fromCart || "/");
    } catch (err: unknown) {
      if (err instanceof HttpError && err.status === 409) {
        setError("An account with that email already exists.");
      } else {
        setError(err instanceof Error ? err.message : "Could not sign up.");
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <Layout>
      <div className="auth-panel">
        <h1>Sign up</h1>
        <p className="muted">Creates a shopper account. Admin is seeded separately and cannot be registered here.</p>
        <form className="card" onSubmit={(event) => void submit(event)}>
          <label className="field">
            Name
            <input name="displayName" required autoComplete="name" />
          </label>
          <label className="field">
            Email
            <input name="email" type="email" required autoComplete="email" />
          </label>
          <label className="field">
            Password
            <input name="password" type="password" required minLength={8} autoComplete="new-password" />
          </label>
          <StatusBanner error={error} />
          <button className="btn btn-auth" type="submit" disabled={busy}>
            {busy ? "Creating account…" : "Sign up"}
          </button>
        </form>
        <p>
          <Link to="/login">Log in</Link>
        </p>
      </div>
    </Layout>
  );
}
