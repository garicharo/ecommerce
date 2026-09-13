import { useEffect, useState } from "react";
import { initials, productImage, relatedProductImage } from "../types";

type Props = {
  sku: string;
  name: string;
  category?: string;
  className?: string;
};

type Stage = "related" | "picsum" | "done";

export function ProductImage({ sku, name, category = "", className }: Props) {
  const [stage, setStage] = useState<Stage>("related");

  useEffect(() => {
    setStage("related");
  }, [sku, name, category]);

  if (stage === "done") {
    return <div className={`img-fallback ${className ?? ""}`}>{initials(name)}</div>;
  }

  const src =
    stage === "related" ? relatedProductImage(name, category, sku) : productImage(sku);

  return (
    <img
      className={className}
      alt={name}
      src={src}
      onError={() => setStage((current) => (current === "related" ? "picsum" : "done"))}
    />
  );
}
