import { createElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Dashboard } from "./App.jsx";
import { api } from "./apiClient.js";

vi.mock("./apiClient.js", () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
  clearSession: vi.fn(),
  getStoredSession: vi.fn(() => null),
  SESSION_EXPIRED_EVENT: "repusense:session-expired",
}));

const searches = [
  {
    id: 101,
    city: "Córdoba",
    category: "Restaurante",
    status: "COMPLETED",
    foundCount: 3,
    savedCount: 2,
    startedAt: "2026-11-12T10:00:00Z",
  },
  {
    id: 102,
    city: "La Plata",
    category: "Farmacia",
    status: "COMPLETED",
    foundCount: 1,
    savedCount: 1,
    startedAt: "2026-11-12T09:00:00Z",
  },
];

function renderDashboard(role) {
  return render(
    createElement(Dashboard, {
      user: { id: 1, name: "Test", email: "test@example.com", role },
      onLogout: vi.fn(),
    })
  );
}

async function openHistory(user) {
  await user.click(await screen.findByRole("button", { name: /HISTORIAL/i }));
  await screen.findByText("Córdoba");
}

describe("search history management", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    api.get.mockImplementation((url) => {
      if (url === "/api/v1/leads") return Promise.resolve({ data: [] });
      if (url === "/api/searches") return Promise.resolve({ data: searches });
      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });
    api.delete.mockResolvedValue({});
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("lets administrators confirm an individual delete and clear the remaining history", async () => {
    const user = userEvent.setup();
    renderDashboard("ADMIN");
    await openHistory(user);

    await user.click(
      screen.getByRole("button", { name: "Eliminar búsqueda #101" })
    );
    expect(window.confirm).toHaveBeenCalledWith(
      expect.stringContaining("Los negocios y oportunidades comerciales se conservarán")
    );
    expect(api.delete).toHaveBeenCalledWith("/api/searches/101");
    expect(await screen.findByText(/La búsqueda fue eliminada/)).toBeVisible();
    await waitFor(() => {
      expect(screen.queryByText("Córdoba")).not.toBeInTheDocument();
    });
    expect(screen.getByText("La Plata")).toBeVisible();

    await user.click(screen.getByRole("button", { name: "Limpiar historial" }));
    expect(window.confirm).toHaveBeenLastCalledWith(
      expect.stringContaining("Se eliminarán todos los registros del historial")
    );
    expect(api.delete).toHaveBeenLastCalledWith("/api/searches");
    expect(
      await screen.findByText("Sin búsquedas registradas")
    ).toBeVisible();
  });

  it("does not show history deletion controls to regular users", async () => {
    const user = userEvent.setup();
    renderDashboard("USER");
    await openHistory(user);

    expect(
      screen.queryByRole("button", { name: "Limpiar historial" })
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Eliminar búsqueda #101" })
    ).not.toBeInTheDocument();
    expect(api.delete).not.toHaveBeenCalled();
  });
});
