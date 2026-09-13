import { type ReactNode } from "react";

type BannerProps = {
  error?: string;
  message?: string;
};

export function StatusBanner({ error, message }: BannerProps) {
  const text = error || message;
  if (!text) {
    return null;
  }
  return <p className={`banner copy-center${error ? " err" : ""}`}>{text}</p>;
}

type ListProps = {
  loading: boolean;
  error?: string;
  empty: boolean;
  emptyCopy: ReactNode;
  children: ReactNode;
};

export function ListStatus({ loading, error, empty, emptyCopy, children }: ListProps) {
  return (
    <>
      <StatusBanner error={error} />
      {loading ? (
        <div className="loader" role="status">
          <span className="spinner" aria-hidden="true" />
          <span className="sr-only">Loading</span>
        </div>
      ) : null}
      {!loading && empty ? <p className="muted copy-center">{emptyCopy}</p> : null}
      {!loading && !empty ? children : null}
    </>
  );
}
