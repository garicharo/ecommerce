import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api";
import { StatusBanner } from "../components/StatusBanner";
import { formatWhen, type ImportJob, type ImportRow, type PageResponse } from "../types";

const TABS = ["INSERTED", "UPDATED", "FAILED", "SKIPPED"] as const;

const ISSUE_LABELS: Record<string, string> = {
  INVALID_HEADER: "Wrong or missing columns",
  EMPTY_NAME: "Missing name",
  EMPTY_SKU: "Missing SKU",
  EMPTY_CATEGORY: "Missing category",
  EMPTY_DESCRIPTION: "Missing description",
  INVALID_PRICE: "Price must be a plain decimal",
  NEGATIVE_STOCK: "Stock below zero",
  INVALID_STOCK: "Stock must be a whole number",
  EMPTY_WEIGHT: "Missing weight",
  INVALID_WEIGHT: "Weight must be a number",
  DUPLICATE_SKU_UPSERT: "Duplicate SKU in the file",
  EMPTY_ROW: "Empty row",
};

function issueLabel(code: string): string {
  if (ISSUE_LABELS[code]) {
    return ISSUE_LABELS[code];
  }
  const words = code.replaceAll("_", " ").toLowerCase();
  return words ? words.charAt(0).toUpperCase() + words.slice(1) : code;
}

function statusLabel(status: string): string {
  if (status === "FAILED_HEADER") {
    return "Header failed";
  }
  return status.charAt(0) + status.slice(1).toLowerCase();
}

export function AdminImportJob() {
  const { jobId = "" } = useParams();
  const [job, setJob] = useState<ImportJob | null>(null);
  const [outcome, setOutcome] = useState<(typeof TABS)[number]>("FAILED");
  const [rows, setRows] = useState<ImportRow[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    let timer = 0;
    let cancelled = false;

    async function poll() {
      try {
        const next = await api.get<ImportJob>(`/api/admin/imports/${jobId}`);
        if (cancelled) {
          return;
        }
        setJob(next);
        if (next.status === "QUEUED" || next.status === "RUNNING") {
          timer = window.setTimeout(() => void poll(), 1000);
        }
      } catch (err: unknown) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Could not load job");
        }
      }
    }

    void poll();
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [jobId]);

  useEffect(() => {
    if (!job || job.status === "QUEUED" || job.status === "RUNNING") {
      return;
    }
    api
      .get<PageResponse<ImportRow>>(`/api/admin/imports/${jobId}/rows?outcome=${outcome}&size=50`)
      .then((page) => setRows(page.items ?? []))
      .catch((err: unknown) => setError(err instanceof Error ? err.message : "Could not load rows"));
  }, [job, jobId, outcome]);

  const running = job && (job.status === "QUEUED" || job.status === "RUNNING");

  return (
    <div className="job-detail">
      <p className="job-back">
        <Link to="/admin/imports">← All jobs</Link>
      </p>
      <StatusBanner error={error} />
      {!job ? <p className="copy-center">Loading…</p> : null}
      {job ? (
        <section className="job-summary">
          <div className="job-summary-head">
            <h2 className="job-filename">{job.filename}</h2>
            <span
              className={`job-status-pill${
                job.status === "FAILED_HEADER" ? " is-failed" : running ? " is-busy" : ""
              }`}
            >
              {statusLabel(job.status)}
            </span>
          </div>
          <p className="muted job-meta">
            {job.totalRows ? `${job.totalRows} rows` : running ? "Counting rows…" : "No rows"}
            {job.createdAt ? ` · ${formatWhen(job.createdAt)}` : ""}
          </p>
          <p className="job-lede">{running ? job.headline || "Import running…" : job.headline}</p>
          {job.errorsGrouped?.length ? (
            <div className="job-issues">
              <h3>What failed</h3>
              <table>
                <thead>
                  <tr>
                    <th>Issue</th>
                    <th className="num">Rows</th>
                  </tr>
                </thead>
                <tbody>
                  {job.errorsGrouped.map((group) => (
                    <tr key={group.code}>
                      <td title={group.code}>{issueLabel(group.code)}</td>
                      <td className="num">{group.count}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
      ) : null}
      {job && !running ? (
        <>
          <p className="job-tabs">
            {TABS.map((tab) => {
              const count =
                tab === "INSERTED"
                  ? job.inserted
                  : tab === "UPDATED"
                    ? job.updated
                    : tab === "FAILED"
                      ? job.failed
                      : job.skipped;
              const label =
                tab === "INSERTED"
                  ? "New"
                  : tab === "UPDATED"
                    ? "Updated"
                    : tab === "FAILED"
                      ? "Errors"
                      : "Skipped";
              return (
                <button
                  key={tab}
                  type="button"
                  className={tab === outcome ? "active" : undefined}
                  onClick={() => setOutcome(tab)}
                >
                  {label} {count}
                </button>
              );
            })}
          </p>
          {rows.length === 0 ? (
            <p className="muted copy-center">No rows in this tab.</p>
          ) : (
            <div className="catalog-table-wrap">
              <table className="catalog">
                <thead>
                  <tr>
                    <th>CSV line</th>
                    <th>SKU</th>
                    <th>Outcome</th>
                    <th>Code</th>
                    <th>Message</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr key={`${row.lineNumber}-${row.sku ?? ""}-${row.outcome}`}>
                      <td>{row.lineNumber}</td>
                      <td>{row.sku}</td>
                      <td>{row.outcome}</td>
                      <td>{row.code}</td>
                      <td className="desc-cell" title={row.message ?? ""}>
                        {row.message}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      ) : null}
    </div>
  );
}
