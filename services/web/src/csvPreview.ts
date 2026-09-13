const REQUIRED = ["name", "sku", "description", "category", "price", "stock", "weight_kg"];

export function parseCsvRecords(text: string): string[][] {
  const records: string[][] = [];
  let field = "";
  let record: string[] = [];
  let inQuotes = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (c === undefined) {
      continue;
    }
    if (inQuotes) {
      if (c === '"') {
        if (text[i + 1] === '"') {
          field += '"';
          i++;
        } else {
          inQuotes = false;
        }
      } else {
        field += c;
      }
    } else if (c === '"') {
      inQuotes = true;
    } else if (c === ",") {
      record.push(field);
      field = "";
    } else if (c === "\n" || c === "\r") {
      if (c === "\r" && text[i + 1] === "\n") {
        i++;
      }
      record.push(field);
      field = "";
      records.push(record);
      record = [];
    } else {
      field += c;
    }
  }
  if (inQuotes) {
    return records;
  }
  if (field.length || record.length) {
    record.push(field);
    records.push(record);
  }
  return records;
}

export function parsePreview(text: string): { header: string[]; rows: string[][]; missing: string[] } {
  const records = parseCsvRecords(text);
  if (!records.length) {
    return { header: [], rows: [], missing: [...REQUIRED] };
  }
  const header = (records[0] ?? []).map((h) => h.trim());
  const rows: string[][] = [];
  for (let i = 1; i < records.length && rows.length < 10; i++) {
    const row = records[i];
    if (!row || row.every((cell) => !String(cell).trim())) {
      continue;
    }
    rows.push(row);
  }
  const missing = REQUIRED.filter((c) => !header.includes(c));
  return { header, rows, missing };
}
