import { DragEvent, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api";
import { parsePreview } from "../csvPreview";
import { StatusBanner } from "./StatusBanner";

export function CsvDrop() {
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);
  const [over, setOver] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [header, setHeader] = useState<string[]>([]);
  const [rows, setRows] = useState<string[][]>([]);
  const [warn, setWarn] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function onFile(next: File) {
    setFile(next);
    setError("");
    const text = await next.slice(0, 64 * 1024).text();
    const preview = parsePreview(text);
    setHeader(preview.header);
    setRows(preview.rows);
    setWarn(
      preview.missing.length
        ? `Preview header may be wrong (missing: ${preview.missing.join(", ")}). You can still upload; the server decides.`
        : "",
    );
  }

  function drop(event: DragEvent) {
    event.preventDefault();
    setOver(false);
    const next = event.dataTransfer.files[0];
    if (next) {
      void onFile(next);
    }
  }

  function pickAnother() {
    setFile(null);
    setHeader([]);
    setRows([]);
    setWarn("");
    setError("");
    if (inputRef.current) {
      inputRef.current.value = "";
      inputRef.current.click();
    }
  }

  async function upload() {
    if (!file) {
      return;
    }
    setBusy(true);
    setError("");
    const fd = new FormData();
    fd.append("file", file);
    try {
      const body = await api.post<{ jobId: string }>("/api/admin/imports", fd);
      navigate(`/admin/imports/${body.jobId}`);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Upload failed");
      setBusy(false);
    }
  }

  return (
    <div className="csv-panel">
      <StatusBanner error={error} />
      {file ? null : (
        <button type="button" className="drop-hit" onClick={() => inputRef.current?.click()}>
          Choose a CSV file
        </button>
      )}
      <div
        className={`drop${over ? " over" : ""}${header.length ? " has-preview" : ""}`}
        onDragOver={(event) => {
          event.preventDefault();
          setOver(true);
        }}
        onDragLeave={() => setOver(false)}
        onDrop={drop}
        onClick={() => {
          if (!file) {
            inputRef.current?.click();
          }
        }}
      >
        {header.length ? (
          <div className="preview-wrap" onClick={(event) => event.stopPropagation()}>
            {file ? <p className="muted preview-file">{file.name}</p> : null}
            <div className="preview-scroll">
              <table className="preview">
                <thead>
                  <tr>
                    {header.map((h) => (
                      <th key={h}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row, i) => (
                    <tr key={i}>
                      {header.map((_h, col) => (
                        <td key={col}>{row[col] ?? ""}</td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : (
          <>
            <span className="drop-hint-desktop">
              Drop a CSV here or click to choose a file. Maximum size 2 MB.
            </span>
            <span className="drop-hint-mobile">{file ? file.name : "Choose a CSV file"}</span>
          </>
        )}
      </div>
      <input
        ref={inputRef}
        className="sr-only"
        type="file"
        accept=".csv,text/csv"
        onChange={(event) => {
          const next = event.target.files?.[0];
          if (next) {
            void onFile(next);
          }
        }}
      />
      <StatusBanner error={warn} />
      {file ? (
        <div className="csv-actions dual">
          <button className="btn" type="button" disabled={busy} onClick={() => void upload()}>
            {busy ? "Uploading…" : "Upload products"}
          </button>
          <button className="btn secondary" type="button" disabled={busy} onClick={pickAnother}>
            Load another file
          </button>
        </div>
      ) : null}
    </div>
  );
}
