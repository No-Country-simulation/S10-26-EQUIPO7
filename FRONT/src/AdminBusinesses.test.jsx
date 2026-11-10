import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen, waitFor, within } from "@testing-library/react";
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

const businesses = [
  {
    id: 11,
    name: "Farmacia Central",
    city: "La Plata",
    category: "Farmacia",
    address: "Calle 1 123",
    rating: 4.7,
    reviewCount: 48,
    leadStatus: "NEW",
  },
  {
    id: 12,
    name: "Mercado Norte",
    city: "Córdoba",
    category: "Alimentos",
    address: "Av. Norte 45",
    rating: 4.2,
    reviewCount: 15,
    leadStatus: null,
  },
];

function mockDashboardData(adminBusinesses = businesses) {
  api.get.mockImplementation((url) => {
    if (url === "/api/v1/leads") return Promise.resolve({ data: [] });
    if (url === "/api/searches") return Promise.resolve({ data: [] });
    if (url === "/api/admin/businesses") {
      return Promise.resolve({ data: adminBusinesses });
    }
    return Promise.reject(new Error(`Unexpected GET ${url}`));
  });
}

function renderDashboard(role = "ADMIN") {
  return render(
    React.createElement(Dashboard, {
      user: { id: 1, name: "Test", email: "test@example.com", role },
      onLogout: vi.fn(),
    })
  );
}

describe("business administration", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockDashboardData();
  });

  afterEach(() => cleanup());

  it("shows the administration navigation only to ADMIN users", async () => {
    const adminView = renderDashboard("ADMIN");
    expect(await screen.findByRole("button", { name: /ADMINISTRACIÓN/i })).toBeVisible();
    adminView.unmount();

    renderDashboard("USER");
    await waitFor(() => {
      expect(screen.queryByRole("button", { name: /ADMINISTRACIÓN/i })).not.toBeInTheDocument();
    });
  });

  it("loads real API data, filters by name, city and category, and selects visible rows", async () => {
    const user = userEvent.setup();
    renderDashboard();
    await user.click(await screen.findByRole("button", { name: /ADMINISTRACIÓN/i }));
    expect(await screen.findByText("Farmacia Central")).toBeVisible();
    expect(screen.getByText("Calle 1 123")).toBeVisible();
    expect(screen.getByText("4.7/5")).toBeVisible();
    expect(screen.getByText("48")).toBeVisible();

    await user.type(screen.getByRole("textbox", { name: "Buscar por nombre" }), "mercado");
    expect(screen.getByText("Mercado Norte")).toBeVisible();
    expect(screen.queryByText("Farmacia Central")).not.toBeInTheDocument();

    await user.clear(screen.getByRole("textbox", { name: "Buscar por nombre" }));
    await user.type(screen.getByRole("textbox", { name: "Filtrar por ciudad" }), "la plata");
    expect(screen.getByText("Farmacia Central")).toBeVisible();
    expect(screen.queryByText("Mercado Norte")).not.toBeInTheDocument();

    await user.clear(screen.getByRole("textbox", { name: "Filtrar por ciudad" }));
    await user.type(screen.getByRole("textbox", { name: "Filtrar por rubro" }), "alimentos");
    expect(screen.getByText("Mercado Norte")).toBeVisible();

    await user.clear(screen.getByRole("textbox", { name: "Filtrar por rubro" }));
    await user.click(screen.getByRole("checkbox", { name: "Seleccionar Farmacia Central" }));
    expect(screen.getByText(/1 seleccionados/)).toBeVisible();
    await user.click(screen.getByRole("checkbox", { name: "Seleccionar todos los comercios visibles" }));
    expect(screen.getByText(/2 seleccionados/)).toBeVisible();
    expect(api.get).toHaveBeenCalledWith("/api/admin/businesses");
  });

  it("requires confirmation, supports cancellation, deletes selected businesses and refreshes the count", async () => {
    const user = userEvent.setup();
    api.delete.mockResolvedValue({
      data: { deletedCount: 2, deletedIds: [11, 12], message: "Eliminados." },
    });
    renderDashboard();
    await user.click(await screen.findByRole("button", { name: /ADMINISTRACIÓN/i }));
    await screen.findByText("Farmacia Central");
    await user.click(screen.getByRole("checkbox", { name: "Seleccionar todos los comercios visibles" }));
    await user.click(screen.getByRole("button", { name: /Eliminar seleccionados \(2\)/i }));

    const dialog = screen.getByRole("alertdialog");
    expect(within(dialog).getByText("Farmacia Central")).toBeVisible();
    expect(within(dialog).getByText("Mercado Norte")).toBeVisible();
    await user.click(within(dialog).getByRole("button", { name: "Cancelar" }));
    expect(api.delete).not.toHaveBeenCalled();

    await user.click(screen.getByRole("button", { name: /Eliminar seleccionados \(2\)/i }));
    await user.click(screen.getByRole("button", { name: "Confirmar eliminación" }));
    await waitFor(() => expect(api.delete).toHaveBeenCalledWith(
      "/api/admin/businesses/bulk",
      { data: { ids: [11, 12] } }
    ));
    expect(await screen.findByRole("status")).toHaveTextContent("Eliminados.");
    expect(screen.queryByText("Farmacia Central")).not.toBeInTheDocument();
    expect(screen.queryByText("Mercado Norte")).not.toBeInTheDocument();
    expect(screen.getByText(/0 visibles · 0 registros totales/)).toBeVisible();
  });

  it("confirms individual deletion and displays API errors without removing the row", async () => {
    const user = userEvent.setup();
    api.delete.mockRejectedValue({
      response: { data: { message: "No se pudo eliminar el comercio." } },
    });
    renderDashboard();
    await user.click(await screen.findByRole("button", { name: /ADMINISTRACIÓN/i }));
    await screen.findByText("Farmacia Central");
    await user.click(screen.getByRole("button", { name: "Eliminar Farmacia Central" }));
    const dialog = screen.getByRole("alertdialog");
    expect(within(dialog).getByText("Farmacia Central")).toBeVisible();
    await user.click(within(dialog).getByRole("button", { name: "Confirmar eliminación" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No se pudo eliminar el comercio."
    );
    expect(screen.getByRole("row", { name: /Farmacia Central/ })).toBeVisible();
  });
});
