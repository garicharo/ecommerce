import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { CsvDrop } from "../components/CsvDrop";
import { ListStatus } from "../components/StatusBanner";
import { api } from "../api";
import { formatWhen, formatWhenDay, jobStatusLabel, type ImportJob, type PageResponse } from "../types";

export function AdminImports() {
  const navigate = useNavigate();
  const [jobs, setJobs] = useState<ImportJob[]>([]);
  const [error, setError] = useState("");
  const [status, setStatus] = useState<"loading" | "ok">("loading");

  useEffect(() => {
    api
      .get<PageResponse<ImportJob>>("/api/admin/imports")
      .then((page) => {
        setJobs(page.items ?? []);
        setStatus("ok");
      })
      .catch((err: unknown) => {
        setError(err instanceof Error ? err.message : "Could not load jobs");
        setStatus("ok");
      });
  }, []);

  return (
    <>
      <CsvDrop />
      <ListStatus
        loading={status === "loading"}
        error={error}
        empty={status === "ok" && jobs.length === 0}
        emptyCopy="No import jobs yet."
      >
        <div className="catalog-table-wrap">
          <table className="catalog jobs">
            <colgroup>
              <col className="job-file" />
              <col className="job-status" />
              <col className="job-result" />
              <col className="job-started" />
            </colgroup>
            <thead>
              <tr>
                <th>File</th>
                <th>Status</th>
                <th>Result</th>
                <th>Started</th>
              </tr>
            </thead>
            <tbody>
              {jobs.map((job) => {
                const href = `/admin/imports/${job.jobId}`;
                const started = job.createdAt ? formatWhen(job.createdAt) : "";
                const tip = [job.headline, started ? `Started ${started}.` : "", "Click to open."].filter(Boolean).join(" ");
                return (
                  <tr
                    key={job.jobId}
                    className="job-row"
                    title={tip}
                    onClick={() => navigate(href)}
                  >
                    <td className="job-file">
                      <Link to={href} onClick={(event) => event.stopPropagation()}>
                        {job.filename}
                      </Link>
                    </td>
                    <td className="job-status">{jobStatusLabel(job.status)}</td>
                    <td className="job-result">{job.headline}</td>
                    <td className="num job-started">{job.createdAt ? formatWhenDay(job.createdAt) : "—"}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </ListStatus>
    </>
  );
}
