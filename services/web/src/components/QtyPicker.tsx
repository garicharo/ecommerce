type Props = {
  stock: number;
  value: number;
  onChange: (qty: number) => void;
  disabled?: boolean;
  invert?: boolean;
  compact?: boolean;
  maxAdd?: number;
};

export function QtyPicker({ stock, value, onChange, disabled, invert, compact, maxAdd }: Props) {
  const available = Math.max(stock, 0);
  const cap = Math.max(0, maxAdd ?? available);

  function setFrom(raw: string) {
    const n = Number(raw);
    if (!Number.isFinite(n)) {
      return;
    }
    if (cap === 0) {
      onChange(0);
      return;
    }
    onChange(Math.min(Math.max(1, Math.floor(n)), cap));
  }

  return (
    <div className={`qty-picker${compact ? " compact" : ""}`}>
      {available === 0 ? (
        <p className={`available${invert ? " invert" : ""}`}>Out of stock</p>
      ) : (
        <>
          <label>
            <span className={compact ? "sr-only" : undefined}>Quantity</span>
            <input
              type="number"
              min={1}
              max={Math.max(cap, 1)}
              value={value}
              disabled={disabled || cap === 0}
              onChange={(event) => setFrom(event.target.value)}
            />
          </label>
          <p className={`available${invert ? " invert" : ""}`}>{`${available} available`}</p>
        </>
      )}
    </div>
  );
}
