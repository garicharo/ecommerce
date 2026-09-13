import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { CartLineQty } from "./CartLineQty";

afterEach(() => {
  cleanup();
});

describe("CartLineQty", () => {
  it("increases quantity and can remove the line", async () => {
    const user = userEvent.setup();
    const onQty = vi.fn();
    const onRemove = vi.fn();
    render(<CartLineQty name="Tent" qty={2} stock={5} onQty={onQty} onRemove={onRemove} />);

    await user.click(screen.getByRole("button", { name: "Increase Tent" }));
    expect(onQty).toHaveBeenCalledWith(3);

    await user.click(screen.getByRole("button", { name: "Remove Tent from cart" }));
    expect(onRemove).toHaveBeenCalledTimes(1);
  });
});
