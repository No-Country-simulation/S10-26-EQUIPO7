import React, { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "./apiClient.js";
import { filterAdminBusinesses } from "./adminBusinessUtils.js";

function getErrorMessage(error) {
  return (
    error.response?.data?.message ||
    error.response?.data?.error ||
    "No se pudo completar la operación. Intentá nuevamente."
  );
}

function formatRating(rating) {
  if (rating === null || rating === undefined || rating === "") {
    return "Sin datos";
  }
  const value = Number(rating);
  return Number.isFinite(value) ? `${value.toFixed(1)}/5` : "Sin datos";
}

export default function AdminBusinesses({ onDataChanged }) {
  const [businesses, setBusinesses] = React.useState([]);
  const [filters, setFilters] = useState({ name: "", city: "", category: "" });
  const [selectedIds, setSelectedIds] = useState(() => new Set());
  const [confirmation, setConfirmation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const loadBusinesses = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const response = await api.get("/api/admin/businesses");
      if (!Array.isArray(response.data)) {
        throw new Error("El backend devolvió una lista de comercios inválida.");
      }
      setBusinesses(response.data);
      setSelectedIds(new Set());
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(loadBusinesses, 0);
    return () => window.clearTimeout(timer);
  }, [loadBusinesses]);

  const visibleBusinesses = useMemo(
    () => filterAdminBusinesses(businesses, filters),
    [businesses, filters]
  );
  const selectedBusinesses = useMemo(
    () => businesses.filter((business) => selectedIds.has(business.id)),
    [businesses, selectedIds]
  );
  const allVisibleSelected = visibleBusinesses.length > 0 &&
    visibleBusinesses.every((business) => selectedIds.has(business.id));

  const toggleBusiness = (id) => {
    setSelectedIds((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleVisible = () => {
    setSelectedIds((current) => {
      const next = new Set(current);
      if (allVisibleSelected) {
        visibleBusinesses.forEach((business) => next.delete(business.id));
      } else {
        visibleBusinesses.forEach((business) => next.add(business.id));
      }
      return next;
    });
  };

  const requestSingleDelete = (business) => {
    setError("");
    setConfirmation({
      ids: [business.id],
      names: [business.name],
    });
  };

  const requestBulkDelete = () => {
    setError("");
    setConfirmation({
      ids: selectedBusinesses.map((business) => business.id),
      names: selectedBusinesses.map((business) => business.name),
    });
  };

  const confirmDelete = async () => {
    if (!confirmation || confirmation.ids.length === 0) return;

    setDeleting(true);
    setError("");
    try {
      const response = confirmation.ids.length === 1
        ? await api.delete(`/api/admin/businesses/${confirmation.ids[0]}`)
        : await api.delete("/api/admin/businesses/bulk", {
            data: { ids: confirmation.ids },
          });
      const deletedIds = Array.isArray(response.data?.deletedIds)
        ? response.data.deletedIds
        : confirmation.ids;

      setBusinesses((current) =>
        current.filter((business) => !deletedIds.includes(business.id))
      );
      setSelectedIds((current) => {
        const next = new Set(current);
        deletedIds.forEach((id) => next.delete(id));
        return next;
      });
      setNotice(
        response.data?.message ||
          `${deletedIds.length} comercio(s) eliminado(s) correctamente.`
      );
      setConfirmation(null);
      await onDataChanged?.();
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="admin-businesses">
      <div className="table-toolbar">
        <div>
          <h3>Administración de comercios</h3>
          <p>
            {visibleBusinesses.length} visibles · {businesses.length} registros
            totales · {selectedIds.size} seleccionados
          </p>
        </div>
        <div className="admin-toolbar-actions">
          {selectedIds.size > 0 && (
            <button
              className="button button-danger"
              disabled={deleting}
              onClick={requestBulkDelete}
              type="button"
            >
              <i className="fa-solid fa-trash" />
              Eliminar seleccionados ({selectedIds.size})
            </button>
          )}
          <button
            className="button button-secondary"
            disabled={loading || deleting}
            onClick={loadBusinesses}
            type="button"
          >
            <i className={`fa-solid ${loading ? "fa-spinner fa-spin" : "fa-rotate"}`} />
            Actualizar
          </button>
        </div>
      </div>

      <div className="admin-filters" aria-label="Filtros de comercios">
        <input
          aria-label="Buscar por nombre"
          onChange={(event) => setFilters((current) => ({
            ...current,
            name: event.target.value,
          }))}
          placeholder="Buscar por nombre"
          value={filters.name}
        />
        <input
          aria-label="Filtrar por ciudad"
          onChange={(event) => setFilters((current) => ({
            ...current,
            city: event.target.value,
          }))}
          placeholder="Filtrar por ciudad"
          value={filters.city}
        />
        <input
          aria-label="Filtrar por rubro"
          onChange={(event) => setFilters((current) => ({
            ...current,
            category: event.target.value,
          }))}
          placeholder="Filtrar por rubro"
          value={filters.category}
        />
      </div>

      {error && (
        <div className="notice notice-error" role="alert">
          <i className="fa-solid fa-circle-exclamation" />
          <span>{error}</span>
          <button aria-label="Cerrar error" onClick={() => setError("")} type="button">
            ×
          </button>
        </div>
      )}
      {notice && (
        <div className="notice notice-success" role="status">
          <i className="fa-solid fa-circle-check" />
          <span>{notice}</span>
          <button aria-label="Cerrar mensaje" onClick={() => setNotice("")} type="button">
            ×
          </button>
        </div>
      )}

      {loading ? (
        <div className="empty-state" role="status">
          <i className="fa-solid fa-spinner fa-spin" />
          <p>Cargando comercios desde el backend...</p>
        </div>
      ) : businesses.length === 0 && !error ? (
        <div className="empty-state">
          <div className="empty-icon"><i className="fa-solid fa-store-slash" /></div>
          <h4>No hay comercios registrados</h4>
          <p>Los comercios almacenados en PostgreSQL aparecerán en esta tabla.</p>
        </div>
      ) : !error && visibleBusinesses.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon"><i className="fa-solid fa-filter-circle-xmark" /></div>
          <h4>No hay resultados para estos filtros</h4>
          <p>Probá con otro nombre, ciudad o rubro.</p>
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>
                  <input
                    aria-label="Seleccionar todos los comercios visibles"
                    checked={allVisibleSelected}
                    onChange={toggleVisible}
                    type="checkbox"
                  />
                </th>
                <th>Comercio</th>
                <th>Ciudad</th>
                <th>Rubro</th>
                <th>Dirección</th>
                <th>Calificación</th>
                <th>Reseñas</th>
                <th>Estado del lead</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {visibleBusinesses.map((business) => (
                <tr key={business.id}>
                  <td>
                    <input
                      aria-label={`Seleccionar ${business.name}`}
                      checked={selectedIds.has(business.id)}
                      onChange={() => toggleBusiness(business.id)}
                      type="checkbox"
                    />
                  </td>
                  <td><span className="business-name">{business.name}</span></td>
                  <td>{business.city || "—"}</td>
                  <td>{business.category || "—"}</td>
                  <td>{business.address || "—"}</td>
                  <td>{formatRating(business.rating)}</td>
                  <td>{business.reviewCount ?? "Sin datos"}</td>
                  <td>
                    {business.leadStatus ? (
                      <span className={`status status-${business.leadStatus.toLowerCase()}`}>
                        {business.leadStatus}
                      </span>
                    ) : <span className="muted">Sin lead</span>}
                  </td>
                  <td>
                    <button
                      aria-label={`Eliminar ${business.name}`}
                      className="icon-button danger-icon-button"
                      disabled={deleting}
                      onClick={() => requestSingleDelete(business)}
                      title={`Eliminar ${business.name}`}
                      type="button"
                    >
                      <i className="fa-solid fa-trash" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {confirmation && (
        <div className="modal-backdrop">
          <section
            aria-labelledby="delete-confirmation-title"
            aria-modal="true"
            className="delete-confirmation-modal"
            role="alertdialog"
          >
            <div className="modal-header">
              <div>
                <span className="eyebrow">CONFIRMAR ELIMINACIÓN</span>
                <h3 id="delete-confirmation-title">
                  {confirmation.ids.length === 1
                    ? "Eliminar comercio"
                    : `Eliminar ${confirmation.ids.length} comercios`}
                </h3>
              </div>
            </div>
            <div className="delete-confirmation-content">
              <p>Se eliminarán los siguientes comercios y sus leads/reseñas asociados:</p>
              <ul>
                {confirmation.names.slice(0, 5).map((name) => <li key={name}>{name}</li>)}
                {confirmation.names.length > 5 && (
                  <li>y {confirmation.names.length - 5} más</li>
                )}
              </ul>
              <p className="muted">
                El historial de búsquedas se conservará.
              </p>
              <div className="admin-confirmation-actions">
                <button
                  className="button button-secondary"
                  disabled={deleting}
                  onClick={() => setConfirmation(null)}
                  type="button"
                >
                  Cancelar
                </button>
                <button
                  className="button button-danger"
                  disabled={deleting}
                  onClick={confirmDelete}
                  type="button"
                >
                  {deleting ? "Eliminando..." : "Confirmar eliminación"}
                </button>
              </div>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
