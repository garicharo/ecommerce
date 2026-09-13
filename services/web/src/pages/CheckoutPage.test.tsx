import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { AuthProvider } from "../AuthProvider";
import { HttpError, api } from "../api";
import { CartProvider } from "../cart";
import { CheckoutPage } from "./CheckoutPage";

vi.mock("../api", async () => {
  const actual = await vi.importActual<typeof import("../api")>("../api");
  return {
    ...actual,
    api: {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      del: vi.fn(),
    },
  };
});

afterEach(() => {
  cleanup();
  sessionStorage.clear();
});

beforeEach(() => {
  sessionStorage.clear();
  vi.mocked(api.get).mockImplementation(async (path: string) => {
    if (path.startsWith("/api/auth/me")) {
      return { email: "sam@shop.local", role: "SHOPPER", displayName: "Sam" } as never;
    }
    if (path.startsWith("/api/cart")) {
      return {
        items: [{ sku: "CT-005", qty: 2, name: "Camping Tent", price: "199.99", stock: 1 }],
      } as never;
    }
    return { items: [] } as never;
  });
  vi.mocked(api.post).mockRejectedValue(
    new HttpError(409, {
      code: "INSUFFICIENT_STOCK",
      message: "Not enough stock",
      details: [{ sku: "CT-005", requested: 2, available: 1 }],
    }),
  );
});

describe("CheckoutPage", () => {
  it("stores stock details and leaves checkout on 409", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={["/checkout"]}>
        <AuthProvider>
          <CartProvider>
            <CheckoutPage />
          </CartProvider>
        </AuthProvider>
      </MemoryRouter>,
    );

    await screen.findByRole("button", { name: "Buy" }, { timeout: 8000 });
    await user.click(screen.getByRole("button", { name: "Buy" }));
    await waitFor(() => {
      expect(sessionStorage.getItem("stockError")).toContain("CT-005");
    });
    expect(api.post).toHaveBeenCalled();
  });
});
