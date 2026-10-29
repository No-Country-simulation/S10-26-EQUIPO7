import { useCallback, useEffect, useMemo, useState } from "react";
import axios from "axios";
import "./App.css";

const API_BASE = (
  import.meta.env.VITE_API_URL || "http://localhost:8080"
).replace(/\/$/, "");

const API = `${API_BASE}/api/v1`;
const SEARCH_API = `${API_BASE}/api/searches`;

const emptySearch = {
  city: "La Plata",
  category: "farmacia",
};

function getLeads(response) {
  const data = response?.data;

  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  if (Array.isArray(data?.data)) return data.data;

  return [];
}

function businessOf(lead) {
  return lead?.business || lead?.businesses || {};
}

function leadStatus(status) {
  const value = String(status || "NEW").toUpperCase();

  if (value === "CONTACTED") return "Contactado";
  if (value === "ANALYZED") return "Analizado";
  if (value === "FAILED") return "Error";

  return "Nuevo";
}

function problemFromAnalysis(analysis = "") {
  const text = analysis.toLowerCase();

  if (/higiene|limpieza|sanitari|suciedad/.test(text)) return "Higiene";
  if (/demora|espera|tardanza|tiempo de atención/.test(text)) return "Demoras";
  if (/precio|tarifa|costo|cobro/.test(text)) return "Precios";
  if (/atención|atencion|trato|servicio al cliente/.test(text)) return "Atención";

  return "Sin clasificar";
}

function riskFromAnalysis(analysis = "") {
  const text = analysis.toLowerCase();

  if (/crítico|critico|muy negativo|grave|urgente/.test(text)) return 5;
  if (/alto|negativo|mala experiencia|recurrente/.test(text)) return 4;
  if (/moderado|regular|mejorar/.test(text)) return 3;
  if (/positivo|buena experiencia|satisfacción|satisfaccion/.test(text)) return 1;

  return null;
}

function formatDate(value) {
  if (!value) return "—";

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? "—"
    : date.toLocaleString("es-AR");
}

function validRating(value) {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const rating = Number(value);

  return Number.isFinite(rating) && rating > 0 && rating <= 5
    ? rating
    : null;
}

function validReviewCount(value) {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const count = Number(value);

  return Number.isInteger(count) && count > 0
    ? count
    : null;
}

function formatRating(value) {
  const rating = validRating(value);

  return rating === null ? "Sin datos" : rating.toFixed(1);
}

function formatReviewCount(value) {
  const count = validReviewCount(value);

  return count === null ? "Sin datos" : count.toLocaleString("es-AR");
}

function RatingStars({ rating }) {
  const valid = validRating(rating);

  if (valid === null) {
    return <span className="muted">Sin datos</span>;
  }

  return (
    <div
      style={{ display: "flex", flexDirection: "column", gap: "4px" }}
      title={`Calificación recibida: ${formatRating(valid)} sobre 5`}
    >
      <span
        style={{
          display: "flex",
          alignItems: "center",
          gap: "3px",
          color: "#e5a000",
        }}
        role="img"
        aria-label={`Calificación ${formatRating(valid)} sobre 5`}
      >
        {[1, 2, 3, 4, 5].map((star) => {
          const difference = valid - star;
          let icon = "fa-regular fa-star";

          if (difference >= 0) {
            icon = "fa-solid fa-star";
          } else if (difference >= -0.5) {
            icon = "fa-solid fa-star-half-stroke";
          }

          return (
            <i
              key={star}
              className={icon}
              style={{
                color:
                  icon === "fa-regular fa-star"
                    ? "#cbd5e1"
                    : "#e5a000",
              }}
            />
          );
        })}
      </span>

      <span className="cell-main">{formatRating(valid)}/5</span>
    </div>
  );
}

export default function App() {
  const [leads, setLeads] = useState([]);
  const [history, setHistory] = useState([]);
  const [searchForm, setSearchForm] = useState(emptySearch);
  const [cityFilter, setCityFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("todos");
  const [query, setQuery] = useState("");

  const [loadingLeads, setLoadingLeads] = useState(false);
  const [searching, setSearching] = useState(false);
  const [analyzingId, setAnalyzingId] = useState(null);
  const [exporting, setExporting] = useState(false);

  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [connection, setConnection] = useState("checking");

  const [analysisById, setAnalysisById] = useState({});
  const [selectedLead, setSelectedLead] = useState(null);
  const [activeTab, setActiveTab] = useState("leads");

  const loadLeads = useCallback(async () => {
    setLoadingLeads(true);

    try {
      const response = await axios.get(`${API}/leads`, {
        params: { page: 0, size: 100 },
        timeout: 20000,
      });

      const loadedLeads = getLeads(response);
      setLeads(loadedLeads);

      const savedAnalyses = {};

      loadedLeads.forEach((lead) => {
        if (lead.aiAnalysis) {
          savedAnalyses[lead.id] = lead.aiAnalysis;
        }
      });

      setAnalysisById((previous) => ({
        ...previous,
        ...savedAnalyses,
      }));

      setConnection("connected");
      setError("");
    } catch (err) {
      setConnection("disconnected");
      setError(
        err.response?.data?.message ||
          err.response?.data?.error ||
          "No se pudo consultar el backend. Verificá que Spring Boot esté iniciado en el puerto 8080."
      );
    } finally {
      setLoadingLeads(false);
    }
  }, []);

  const loadHistory = useCallback(async () => {
    try {
      const response = await axios.get(SEARCH_API, { timeout: 20000 });

      setHistory(
        Array.isArray(response.data) ? [...response.data].reverse() : []
      );
    } catch {
      setHistory([]);
    }
  }, []);

  useEffect(() => {
    loadLeads();
    loadHistory();
  }, [loadLeads, loadHistory]);

  const businesses = useMemo(
    () =>
      leads.map((lead) => {
        const business = businessOf(lead);
        const analysis = analysisById[lead.id] || lead.aiAnalysis || "";

        return {
          ...lead,
          business,
          displayName: business.name || lead.name || `Comercio #${lead.id}`,
          city: business.city || "Sin ubicación",
          category: business.category || "Sin rubro",
          address: business.address || "",
          website: business.website || "",
          phone: business.phone || "",
          rating: validRating(business.rating),
          reviewCount: validReviewCount(business.reviewCount),
          statusLabel: leadStatus(lead.status),
          analysis,
          problem: analysis
            ? problemFromAnalysis(analysis)
            : "Pendiente de análisis",
          risk: analysis ? riskFromAnalysis(analysis) : null,
        };
      }),
    [leads, analysisById]
  );

  const filteredBusinesses = useMemo(
    () =>
      businesses.filter((item) => {
        const search = query.trim().toLowerCase();

        const matchesQuery =
          !search ||
          [
            item.displayName,
            item.city,
            item.category,
            item.address,
            item.phone,
          ].some((value) =>
            String(value || "").toLowerCase().includes(search)
          );

        const matchesCity =
          !cityFilter ||
          item.city.toLowerCase().includes(cityFilter.toLowerCase());

        const matchesStatus =
          statusFilter === "todos" ||
          String(item.status || "NEW").toUpperCase() === statusFilter;

        return matchesQuery && matchesCity && matchesStatus;
      }),
    [businesses, query, cityFilter, statusFilter]
  );

  const stats = useMemo(
    () => ({
      total: leads.length,
      new: leads.filter(
        (lead) => String(lead.status || "NEW").toUpperCase() === "NEW"
      ).length,
      contacted: leads.filter(
        (lead) => String(lead.status || "").toUpperCase() === "CONTACTED"
      ).length,
      analyzed: leads.filter(
        (lead) => Boolean(lead.aiAnalysis || analysisById[lead.id])
      ).length,
      searches: history.length,
    }),
    [leads, analysisById, history]
  );

  const runSearch = async (event) => {
    event.preventDefault();

    if (!searchForm.city.trim() || !searchForm.category.trim()) {
      setError("Ingresá una ciudad y un rubro para comenzar la búsqueda.");
      return;
    }

    setSearching(true);
    setError("");
    setNotice("");

    try {
      const response = await axios.post(
        SEARCH_API,
        {
          city: searchForm.city.trim(),
          category: searchForm.category.trim(),
        },
        { timeout: 120000 }
      );

      const result = response.data || {};

      setNotice(
        `Búsqueda ${result.status || "completada"}: ${
          result.foundCount ?? 0
        } comercios encontrados y ${
          result.savedCount ?? 0
        } procesados/guardados.`
      );

      await Promise.all([loadLeads(), loadHistory()]);
      setActiveTab("leads");
    } catch (err) {
      const data = err.response?.data;
      const message =
        typeof data === "string"
          ? data
          : data?.error || data?.message || err.message;

      setError(
        `La búsqueda no se pudo completar. ${
          message || "Revisá el backend y la fuente de comercios."
        }`
      );

      await loadHistory();
    } finally {
      setSearching(false);
    }
  };

  const analyzeLead = async (lead) => {
    setAnalyzingId(lead.id);
    setError("");
    setNotice("");

    try {
      const response = await axios.post(
        `${API}/leads/${lead.id}/analyze`,
        {},
        { timeout: 90000 }
      );

      const analysis = response.data?.analysis;

      if (typeof analysis !== "string" || !analysis.trim()) {
        throw new Error("El backend no devolvió un análisis válido.");
      }

      setAnalysisById((previous) => ({
        ...previous,
        [lead.id]: analysis,
      }));

      setLeads((previous) =>
        previous.map((item) =>
          item.id === lead.id
            ? {
                ...item,
                aiAnalysis: analysis,
                status:
                  String(item.status || "").toUpperCase() === "CONTACTED"
                    ? "CONTACTED"
                    : "ANALYZED",
              }
            : item
        )
      );

      setSelectedLead({
        ...lead,
        aiAnalysis: analysis,
        analysis,
        status:
          String(lead.status || "").toUpperCase() === "CONTACTED"
            ? "CONTACTED"
            : "ANALYZED",
      });

      setNotice(`Análisis generado y guardado para ${lead.displayName}.`);
    } catch (err) {
      const data = err.response?.data;
      const message =
        typeof data === "string"
          ? data
          : data?.error || data?.message || err.message;

      setError(`No se pudo analizar ${lead.displayName}. ${message}`);
    } finally {
      setAnalyzingId(null);
    }
  };

  const markContacted = async (lead) => {
    setError("");
    setNotice("");

    try {
      await axios.patch(`${API}/leads/${lead.id}/status`, {
        status: "CONTACTED",
      });

      setLeads((previous) =>
        previous.map((item) =>
          item.id === lead.id ? { ...item, status: "CONTACTED" } : item
        )
      );

      setNotice(
        `${lead.displayName} quedó marcado como contactado. No se envió ningún correo.`
      );
    } catch (err) {
      setError(
        `No se pudo actualizar el estado del lead: ${
          err.response?.data?.message ||
          err.response?.data?.error ||
          err.message
        }`
      );
    }
  };

  const openAnalysis = (lead) => {
    setSelectedLead({
      ...lead,
      analysis:
        analysisById[lead.id] || lead.aiAnalysis || lead.analysis || "",
    });
  };

  const exportCsv = async () => {
    setError("");
    setNotice("");
    setExporting(true);

    try {
      const response = await axios.get(`${API}/leads/export/csv`, {
        responseType: "blob",
        timeout: 30000,
      });

      const blob = new Blob([response.data], {
        type: "text/csv;charset=utf-8;",
      });

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");

      link.href = url;
      link.download = "repusense-leads.csv";
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      setNotice("El archivo CSV se descargó correctamente.");
    } catch (err) {
      setError(
        `No se pudo exportar el CSV. ${
          err.response?.data?.message ||
          err.response?.data?.error ||
          err.message
        }`
      );
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <div className="brand-mark">
            <i className="fa-solid fa-shield-halved" />
          </div>
          <div>
            <h1>RepuSense</h1>
            <p>Radar de reputación y prospección inteligente con IA</p>
          </div>
        </div>

        <div className={`connection-pill ${connection}`}>
          <span className="connection-dot" />
          {connection === "connected"
            ? "Backend conectado"
            : connection === "disconnected"
              ? "Backend desconectado"
              : "Comprobando conexión"}
        </div>
      </header>

      <main className="page-content">
        <section className="welcome-row">
          <div>
            <p className="eyebrow">CENTRO DE CONTROL</p>
            <h2>Panel de prospección</h2>
            <p className="muted">
              Buscá comercios reales, consultá tus leads y analizá la
              información disponible con IA.
            </p>
          </div>

          <button
            className="button button-secondary"
            onClick={() => {
              loadLeads();
              loadHistory();
            }}
            disabled={loadingLeads || searching}
          >
            <i
              className={`fa-solid ${
                loadingLeads ? "fa-spinner fa-spin" : "fa-rotate"
              }`}
            />
            Actualizar datos
          </button>
        </section>

        {error && (
          <div className="notice notice-error">
            <i className="fa-solid fa-circle-exclamation" />
            <span>{error}</span>
            <button onClick={() => setError("")} aria-label="Cerrar">
              ×
            </button>
          </div>
        )}

        {notice && (
          <div className="notice notice-success">
            <i className="fa-solid fa-circle-check" />
            <span>{notice}</span>
            <button onClick={() => setNotice("")} aria-label="Cerrar">
              ×
            </button>
          </div>
        )}

        <section className="stats-grid">
          <article className="stat-card">
            <div className="stat-icon blue">
              <i className="fa-solid fa-store" />
            </div>
            <div>
              <span>Leads disponibles</span>
              <strong>{stats.total}</strong>
              <small>Registros devueltos por el backend</small>
            </div>
          </article>

          <article className="stat-card">
            <div className="stat-icon amber">
              <i className="fa-solid fa-bolt" />
            </div>
            <div>
              <span>Leads nuevos</span>
              <strong>{stats.new}</strong>
              <small>Pendientes de gestión</small>
            </div>
          </article>

          <article className="stat-card">
            <div className="stat-icon green">
              <i className="fa-solid fa-address-card" />
            </div>
            <div>
              <span>Contactados</span>
              <strong>{stats.contacted}</strong>
              <small>Marcados en el sistema</small>
            </div>
          </article>

          <article className="stat-card">
            <div className="stat-icon violet">
              <i className="fa-solid fa-brain" />
            </div>
            <div>
              <span>Análisis recibidos</span>
              <strong>{stats.analyzed}</strong>
              <small>Resultados guardados en el sistema</small>
            </div>
          </article>
        </section>

        <section className="panel search-panel">
          <div className="panel-heading">
            <div>
              <span className="section-icon">
                <i className="fa-solid fa-magnifying-glass-location" />
              </span>
              <div>
                <h3>Buscar comercios</h3>
                <p>
                  La búsqueda se ejecuta en el backend y guarda los
                  resultados válidos en la base de datos.
                </p>
              </div>
            </div>
            <span className="tag tag-blue">Búsqueda real</span>
          </div>

          <form className="search-form" onSubmit={runSearch}>
            <label>
              Ciudad
              <input
                value={searchForm.city}
                onChange={(e) =>
                  setSearchForm({ ...searchForm, city: e.target.value })
                }
                placeholder="Ej. La Plata"
              />
            </label>

            <label>
              Rubro
              <input
                value={searchForm.category}
                onChange={(e) =>
                  setSearchForm({ ...searchForm, category: e.target.value })
                }
                placeholder="Ej. farmacia, hotel, restaurante"
              />
            </label>

            <button
              className="button button-primary"
              type="submit"
              disabled={searching}
            >
              <i
                className={`fa-solid ${
                  searching
                    ? "fa-spinner fa-spin"
                    : "fa-magnifying-glass"
                }`}
              />
              {searching ? "Buscando comercios..." : "Iniciar búsqueda"}
            </button>
          </form>

          {searching && (
            <div className="progress-line">
              <span />
            </div>
          )}

          <p className="help-text">
            <i className="fa-solid fa-circle-info" />
            Las calificaciones y reseñas se muestran solo cuando la fuente
            las proporciona. La búsqueda actual utiliza OpenStreetMap.
          </p>
        </section>

        <section className="panel data-panel">
          <div className="tabs">
            <button
              className={activeTab === "leads" ? "tab active" : "tab"}
              onClick={() => setActiveTab("leads")}
            >
              <i className="fa-solid fa-list-check" />
              Comercios y leads
              <span>{businesses.length}</span>
            </button>

            <button
              className={activeTab === "history" ? "tab active" : "tab"}
              onClick={() => setActiveTab("history")}
            >
              <i className="fa-solid fa-clock-rotate-left" />
              Historial
              <span>{history.length}</span>
            </button>
          </div>

          {activeTab === "leads" ? (
            <>
              <div className="table-toolbar">
                <div>
                  <h3>Comercios identificados</h3>
                  <p>
                    {filteredBusinesses.length} resultados visibles de{" "}
                    {businesses.length} leads cargados
                  </p>
                </div>

                <div className="filters">
                  <input
                    aria-label="Buscar comercio"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Buscar por nombre, rubro..."
                  />

                  <input
                    aria-label="Filtrar ciudad"
                    value={cityFilter}
                    onChange={(e) => setCityFilter(e.target.value)}
                    placeholder="Filtrar ciudad"
                  />

                  <select
                    aria-label="Filtrar estado"
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                  >
                    <option value="todos">Todos los estados</option>
                    <option value="NEW">Nuevo</option>
                    <option value="ANALYZED">Analizado</option>
                    <option value="CONTACTED">Contactado</option>
                    <option value="FAILED">Error</option>
                  </select>

                  <button
                    className="button button-secondary"
                    onClick={exportCsv}
                    disabled={exporting}
                    type="button"
                  >
                    <i
                      className={`fa-solid ${
                        exporting
                          ? "fa-spinner fa-spin"
                          : "fa-file-csv"
                      }`}
                    />
                    {exporting ? "Exportando..." : "Exportar CSV"}
                  </button>
                </div>
              </div>

              {loadingLeads ? (
                <div className="empty-state">
                  <i className="fa-solid fa-spinner fa-spin" />
                  <p>Cargando leads desde el backend...</p>
                </div>
              ) : filteredBusinesses.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-icon">
                    <i className="fa-solid fa-store-slash" />
                  </div>
                  <h4>No hay comercios para mostrar</h4>
                  <p>
                    Ejecutá una búsqueda por ciudad y rubro o cambiá los
                    filtros. No se muestran datos de ejemplo.
                  </p>
                </div>
              ) : (
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Comercio</th>
                        <th>Ubicación / rubro</th>
                        <th>Calificación</th>
                        <th>Cantidad de reseñas</th>
                        <th>Estado</th>
                        <th>Riesgo</th>
                        <th>Problema detectado</th>
                        <th>Acciones</th>
                      </tr>
                    </thead>

                    <tbody>
                      {filteredBusinesses.map((item) => (
                        <tr key={item.id}>
                          <td>
                            <div className="business-name">
                              {item.displayName}
                            </div>
                            <div className="subtext">
                              {item.address ||
                                item.phone ||
                                `Lead #${item.id}`}
                            </div>
                          </td>

                          <td>
                            <span className="cell-main">{item.city}</span>
                            <span className="subtext">{item.category}</span>
                          </td>

                          <td>
                            <RatingStars rating={item.rating} />
                          </td>

                          <td>{formatReviewCount(item.reviewCount)}</td>

                          <td>
                            <span
                              className={`status status-${String(
                                item.status || "NEW"
                              ).toLowerCase()}`}
                            >
                              {item.statusLabel}
                            </span>
                          </td>

                          <td>
                            {item.risk ? (
                              <span className={`risk risk-${item.risk}`}>
                                {item.risk}/5
                              </span>
                            ) : (
                              <span className="muted">Sin evaluar</span>
                            )}
                          </td>

                          <td>{item.problem}</td>

                          <td>
                            <div className="action-buttons">
                              <button
                                className="icon-button"
                                title="Analizar con Gemini"
                                aria-label={`Analizar ${item.displayName}`}
                                onClick={() => analyzeLead(item)}
                                disabled={analyzingId === item.id}
                              >
                                <i
                                  className={`fa-solid ${
                                    analyzingId === item.id
                                      ? "fa-spinner fa-spin"
                                      : "fa-brain"
                                  }`}
                                />
                              </button>

                              <button
                                className="icon-button"
                                title="Ver análisis guardado"
                                aria-label={`Ver análisis de ${item.displayName}`}
                                onClick={() => openAnalysis(item)}
                                disabled={!item.analysis}
                              >
                                <i className="fa-solid fa-eye" />
                              </button>

                              <button
                                className="icon-button"
                                title="Marcar como contactado (no envía correo)"
                                aria-label={`Marcar contactado a ${item.displayName}`}
                                onClick={() => markContacted(item)}
                                disabled={
                                  String(item.status).toUpperCase() ===
                                  "CONTACTED"
                                }
                              >
                                <i className="fa-solid fa-user-check" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          ) : (
            <>
              <div className="table-toolbar">
                <div>
                  <h3>Historial de búsquedas</h3>
                  <p>
                    {history.length} ejecuciones registradas por el backend
                  </p>
                </div>

                <button
                  className="button button-secondary"
                  onClick={loadHistory}
                >
                  <i className="fa-solid fa-rotate" />
                  Actualizar historial
                </button>
              </div>

              {history.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-icon">
                    <i className="fa-solid fa-clock-rotate-left" />
                  </div>
                  <h4>Sin búsquedas registradas</h4>
                  <p>
                    Cuando ejecutes una búsqueda, aparecerá aquí su estado
                    y cantidad de resultados.
                  </p>
                </div>
              ) : (
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Fecha</th>
                        <th>Ciudad</th>
                        <th>Rubro</th>
                        <th>Estado</th>
                        <th>Encontrados</th>
                        <th>Guardados</th>
                      </tr>
                    </thead>

                    <tbody>
                      {history.map((item) => (
                        <tr key={item.id}>
                          <td>{formatDate(item.startedAt)}</td>
                          <td>{item.city || "—"}</td>
                          <td>{item.category || "—"}</td>
                          <td>
                            <span
                              className={`status status-${String(
                                item.status || ""
                              ).toLowerCase()}`}
                            >
                              {item.status || "—"}
                            </span>
                          </td>
                          <td>{item.foundCount ?? 0}</td>
                          <td>{item.savedCount ?? 0}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          )}
        </section>

        <footer className="page-footer">
          <span>RepuSense · Panel de prospección</span>
          <span>
            Los análisis guardados se recuperan desde el backend.
            La evaluación de reputación depende de los datos disponibles.
          </span>
        </footer>
      </main>

      {selectedLead && (
        <div
          className="modal-backdrop"
          onClick={() => setSelectedLead(null)}
          role="presentation"
        >
          <section
            className="analysis-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="analysis-title"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="modal-header">
              <div>
                <span className="eyebrow">ANÁLISIS DE REPUTACIÓN</span>
                <h3 id="analysis-title">{selectedLead.displayName}</h3>
              </div>

              <button
                className="icon-button close-button"
                onClick={() => setSelectedLead(null)}
                aria-label="Cerrar"
              >
                <i className="fa-solid fa-xmark" />
              </button>
            </div>

            {selectedLead.analysis ? (
              <div className="analysis-content">
                <p className="analysis-label">
                  <i className="fa-solid fa-wand-magic-sparkles" />
                  Resultado de Gemini
                </p>
                <p className="analysis-text">{selectedLead.analysis}</p>
              </div>
            ) : (
              <div className="empty-state modal-empty">
                <i className="fa-solid fa-file-circle-question" />
                <h4>Todavía no hay un análisis</h4>
                <p>
                  Seleccioná Analizar con Gemini para generar un análisis
                  con la información disponible del comercio.
                </p>

                <button
                  className="button button-primary"
                  onClick={() => {
                    const lead = selectedLead;
                    setSelectedLead(null);
                    analyzeLead(lead);
                  }}
                  disabled={analyzingId === selectedLead.id}
                >
                  <i className="fa-solid fa-brain" />
                  Analizar con Gemini
                </button>
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}