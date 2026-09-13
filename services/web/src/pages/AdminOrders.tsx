import { useEffect, useState } from "react";
import { OrderCard } from "../components/OrderCard";
import { ListStatus } from "../components/StatusBanner";
import { api } from "../api";
import { type Order, type PageResponse } from "../types";

export function AdminOrders() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [status, setStatus] = useState<"loading" | "ok" | "error">("loading");
  const [error, setError] = useState("");

  useEffect(() => {
    api
      .get<PageResponse<Order>>("/api/admin/orders")
      .then((page) => {
        setOrders(page.items ?? []);
        setStatus("ok");
      })
      .catch((err: unknown) => {
        setStatus("error");
        setError(err instanceof Error ? err.message : "Could not load orders");
      });
  }, []);

  return (
    <>
      <ListStatus
        loading={status === "loading"}
        error={error}
        empty={status === "ok" && orders.length === 0}
        emptyCopy="No orders yet."
      >
        <div className="order-list">
          {orders.map((order) => (
            <OrderCard key={order.id} order={order} showId />
          ))}
        </div>
      </ListStatus>
    </>
  );
}
