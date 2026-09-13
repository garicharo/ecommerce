import { useEffect, useState } from "react";
import { Layout } from "../Layout";
import { OrderCard } from "../components/OrderCard";
import { ListStatus } from "../components/StatusBanner";
import { api } from "../api";
import { type Order, type PageResponse } from "../types";

export function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [status, setStatus] = useState<"loading" | "ok" | "error">("loading");
  const [error, setError] = useState("");

  useEffect(() => {
    api
      .get<PageResponse<Order>>("/api/orders")
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
    <Layout title="Your orders">
      <ListStatus
        loading={status === "loading"}
        error={error}
        empty={status === "ok" && orders.length === 0}
        emptyCopy="No orders yet."
      >
        <div className="order-list inset-10">
          {orders.map((order) => (
            <OrderCard key={order.id} order={order} />
          ))}
        </div>
      </ListStatus>
    </Layout>
  );
}
