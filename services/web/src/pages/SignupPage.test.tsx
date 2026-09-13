import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { AuthProvider } from "../AuthProvider";
import { HttpError, api } from "../api";
import { CartProvider } from "../cart";
import { SignupPage } from "./SignupPage";

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
});

beforeEach(() => {
  vi.mocked(api.get).mockImplementation(async (path: string) => {
    if (path.startsWith("/api/auth/me")) {
      throw new HttpError(401, { code: "UNAUTHORIZED", message: "Not logged in", details: [] });
    }
    return { items: [] } as never;
  });
});

describe("SignupPage", () => {
  it("shows a conflict when the email is taken", async () => {
    const user = userEvent.setup();
    vi.mocked(api.post).mockRejectedValue(
      new HttpError(409, { code: "EMAIL_TAKEN", message: "taken", details: [] }),
    );
    render(
      <MemoryRouter>
        <AuthProvider>
          <CartProvider>
            <SignupPage />
          </CartProvider>
        </AuthProvider>
      </MemoryRouter>,
    );

    await waitFor(() => expect(screen.getByRole("button", { name: "Sign up" })).toBeEnabled());
    await user.type(screen.getByLabelText("Name"), "Ada");
    await user.type(screen.getByLabelText("Email"), "ada@shop.local");
    await user.type(screen.getByLabelText("Password"), "password1");
    await user.click(screen.getByRole("button", { name: "Sign up" }));

    expect(await screen.findByText("An account with that email already exists.")).toBeInTheDocument();
  });
});
