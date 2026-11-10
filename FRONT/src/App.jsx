import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import AuthScreen from "./AuthScreen.jsx";
import AdminBusinesses from "./AdminBusinesses.jsx";
import {
  api,
  clearSession,
  getStoredSession,
  SESSION_EXPIRED_EVENT,
} from "./apiClient.js";
import "./App.css";

const API = "/api/v1";
const SEARCH_API = "/api/searches";

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

function getHistory(response) {
  const data = response?.data;

  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  if (Array.isArray(data?.data)) return data.data;
  if (Array.isArray(data?.searches)) return data.searches;

  return null;
}

function getErrorMessage(err) {
  const data = err?.response?.data;

  if (typeof data === "string") return data;

  return (
    data?.error ||
    data?.message ||
    err?.message ||
    "Ocurrió un error inesperado."
  );
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
  const text = String(analysis).toLowerCase();

  if (/higiene|limpieza|sanitari|suciedad/.test(text)) return "Higiene";
  if (/demora|espera|tardanza|tiempo de atención/.test(text)) return "Demoras";
  if (/precio|tarifa|costo|cobro/.test(text)) return "Precios";
  if (/atención|atencion|trato|servicio al cliente/.test(text)) return "Atención";

  return "Sin clasificar";
}

function riskFromLead(lead) {
  const business = businessOf(lead);

  const value =
    lead?.riskScore ??
    lead?.riskLevel ??
    lead?.risk ??
    business?.riskScore ??
    business?.riskLevel ??
    business?.risk;

  if (value === null || value === undefined || value === "") {
    return null;
  }

  const numeric = Number(value);

  if (Number.isFinite(numeric) && numeric >= 1 && numeric <= 5) {
    return Math.round(numeric);
  }

  const normalized = String(value).trim().toUpperCase();

  const levels = {
    CRITICAL: 5,
    CRITICO: 5,
    CRÍTICO: 5,
    HIGH: 4,
    ALTO: 4,
    MEDIUM: 3,
    MODERADO: 3,
    MEDIO: 3,
    LOW: 2,
    BAJO: 2,
    MINIMAL: 1,
    MINIMO: 1,
    MÍNIMO: 1,
  };

  return levels[normalized] ?? null;
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
  const [session, setSession] = React.useState(getStoredSession);
  const [authMode, setAuthMode] = useState("login");

  useEffect(() => {
    const handleExpiredSession = () => {
      setSession(null);
      setAuthMode("login");
    };

    window.addEventListener(SESSION_EXPIRED_EVENT, handleExpiredSession);
    return () =>
      window.removeEventListener(SESSION_EXPIRED_EVENT, handleExpiredSession);
  }, []);

  const handleLogout = () => {
    clearSession();
    setSession(null);
    setAuthMode("login");
  };

  useEffect(() => {
    if (!session) {
      return undefined;
    }

    let timer;
    const expireSession = () => {
      const remaining = session.expiresAt - Date.now();
      if (remaining <= 0) {
        clearSession();
        setSession(null);
        setAuthMode("login");
        return;
      }

      timer = window.setTimeout(
        expireSession,
        Math.min(remaining, 2147483647)
      );
    };

    timer = window.setTimeout(
      expireSession,
      Math.min(session.expiresAt - Date.now(), 2147483647)
    );

    return () => window.clearTimeout(timer);
  }, [session]);

  if (!session) {
    return (
      <AuthScreen
        mode={authMode}
        onAuthenticated={setSession}
        onModeChange={setAuthMode}
      />
    );
  }

  return (
    <Dashboard
      user={session.user}
      onLogout={handleLogout}
    />
  );
}

export function Dashboard({ user, onLogout }) {
  const [leads, setLeads] = useState([]);
  const [history, setHistory] = useState([]);

  const [searchForm, setSearchForm] = useState(emptySearch);
  const [cityFilter, setCityFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("todos");
  const [query, setQuery] = useState("");

  const [loadingLeads, setLoadingLeads] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [searching, setSearching] = useState(false);
  const [analyzingId, setAnalyzingId] = useState(null);
  const [exporting, setExporting] = useState(false);
  const [exportingSearchIds, setExportingSearchIds] = useState(() => new Set());

  const [error, setError] = useState("");
  const [historyError, setHistoryError] = useState("");
  const [notice, setNotice] = useState("");
  const [connection, setConnection] = useState("checking");

  const [analysisById, setAnalysisById] = useState({});
  const [selectedLead, setSelectedLead] = useState(null);
  const [activeTab, setActiveTab] = useState("leads");

  // Identifica cada consulta del historial para evitar respuestas fuera de orden.
  const historyRequestRef = useRef(0);

  const loadLeads = useCallback(async () => {
    setLoadingLeads(true);

    try {
      const response = await api.get(`${API}/leads`, {
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

      return loadedLeads;
    } catch (err) {
      console.error("Error al cargar leads:", err);
      setConnection("disconnected");

      setError(
        getErrorMessage(err) ||
          "No se pudo consultar el backend. Verificá que Spring Boot esté iniciado en el puerto 8080."
      );

      return null;
    } finally {
      setLoadingLeads(false);
    }
  }, []);

  const loadHistory = useCallback(async () => {
    // Cada consulta recibe un número único.
    const requestId = ++historyRequestRef.current;

    setLoadingHistory(true);
    setHistoryError("");

    try {
      console.log("Consultando historial:", SEARCH_API);

      const response = await api.get(SEARCH_API, {
        timeout: 20000,
      });

      console.log("Respuesta del historial:", response.data);

      const data = getHistory(response);

      if (data === null) {
        throw new Error(
          "El backend no devolvió una lista de búsquedas. Revisá la respuesta de /api/searches."
        );
      }

      const loadedHistory = [...data].sort((a, b) => {
        const dateA = new Date(a.startedAt || 0).getTime();
        const dateB = new Date(b.startedAt || 0).getTime();

        return dateB - dateA;
      });

      // Una respuesta anterior no puede reemplazar una consulta más reciente.
      if (requestId !== historyRequestRef.current) {
        return loadedHistory;
      }

      setHistory(loadedHistory);
      setHistoryError("");

      console.log(
        `Historial actualizado correctamente: ${loadedHistory.length} búsquedas.`
      );

      return loadedHistory;
    } catch (err) {
      console.error("ERROR AL CARGAR HISTORIAL:", err);

      // Ignora errores de consultas que ya fueron reemplazadas.
      if (requestId !== historyRequestRef.current) {
        return null;
      }

      // No borramos el historial anterior si falla una actualización.
      setHistoryError(
        `No se pudo actualizar el historial: ${getErrorMessage(err)}`
      );

      return null;
    } finally {
      // Solo la última consulta puede cambiar el indicador de carga.
      if (requestId === historyRequestRef.current) {
        setLoadingHistory(false);
      }
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadLeads();
      loadHistory();
    }, 0);

    return () => window.clearTimeout(timer);
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
          hasReviews: lead.hasReviews,
          problem: analysis
            ? problemFromAnalysis(analysis)
            : "Pendiente de análisis",
          risk: riskFromLead(lead),
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
    setHistoryError("");
    setNotice("");

    try {
      const response = await api.post(
        SEARCH_API,
        {
          city: searchForm.city.trim(),
          category: searchForm.category.trim(),
        },
        { timeout: 120000 }
      );

      const result = response.data || {};

      const [updatedLeads] = await Promise.all([
        loadLeads(),
        loadHistory(),
      ]);

      setActiveTab("leads");

      if (updatedLeads === null) {
        setNotice(
          `La búsqueda finalizó con estado ${
            result.status || "desconocido"
          }, pero no se pudieron actualizar los leads del panel.`
        );
      } else {
        setNotice(
          `Búsqueda ${result.status || "finalizada"}. ` +
            `Comercios encontrados: ${result.foundCount ?? 0}. ` +
            `Registros procesados: ${result.savedCount ?? 0}. ` +
            `Leads cargados en el panel: ${updatedLeads.length}.`
        );
      }
    } catch (err) {
      const message = getErrorMessage(err);

      setError(`No se pudo completar la búsqueda. ${message}`);
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
      const response = await api.post(
        `${API}/leads/${lead.id}/analyze`,
        {},
        { timeout: 90000 }
      );

      const analysis = response.data?.analysis;
      const hasReviews = response.data?.hasReviews === true;

      if (typeof analysis !== "string" || !analysis.trim()) {
        throw new Error("El backend no devolvió un análisis válido.");
      }

      const updatedLead = {
        ...lead,
        aiAnalysis: analysis,
        analysis,
        hasReviews,
        riskScore:
          response.data?.riskScore ??
          response.data?.riskLevel ??
          lead.riskScore ??
          lead.riskLevel ??
          null,
        status:
          String(lead.status || "").toUpperCase() === "CONTACTED"
            ? "CONTACTED"
            : "ANALYZED",
      };

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
                hasReviews,
                riskScore: updatedLead.riskScore,
                status: updatedLead.status,
              }
            : item
        )
      );

      setSelectedLead({
        ...updatedLead,
        displayName:
          lead.displayName ||
          businessOf(lead).name ||
          `Comercio #${lead.id}`,
      });

      setNotice(
        `Análisis generado con Gemini y guardado para ${
          lead.displayName || `el comercio #${lead.id}`
        }.`
      );
    } catch (err) {
      const message = getErrorMessage(err);

      setError(
        `No se pudo analizar ${
          lead.displayName || `el comercio #${lead.id}`
        }. ${message}`
      );
    } finally {
      setAnalyzingId(null);
    }
  };

  const markContacted = async (lead) => {
    setError("");
    setNotice("");

    try {
      await api.patch(`${API}/leads/${lead.id}/status`, {
        status: "CONTACTED",
      });

      setLeads((previous) =>
        previous.map((item) =>
          item.id === lead.id
            ? { ...item, status: "CONTACTED" }
            : item
        )
      );

      setSelectedLead((previous) =>
        previous?.id === lead.id
          ? { ...previous, status: "CONTACTED" }
          : previous
      );

      setNotice(
        `${lead.displayName} quedó marcado como contactado. No se envió ningún correo.`
      );
    } catch (err) {
      setError(
        `No se pudo actualizar el estado del lead: ${getErrorMessage(err)}`
      );
    }
  };

  const openAnalysis = (lead) => {
    setSelectedLead({
      ...lead,
      analysis:
        analysisById[lead.id] || lead.aiAnalysis || lead.analysis || "",
      hasReviews: lead.hasReviews,
    });
  };

  const exportCsv = async (searchId = null) => {
    setError("");
    setNotice("");

    if (searchId !== null) {
      setExportingSearchIds((currentIds) => new Set(currentIds).add(searchId));
    } else {
      setExporting(true);
    }

    try {
      const response = await api.get(`${API}/leads/export/csv`, {
        params: searchId !== null ? { searchId } : {},
        responseType: "blob",
        timeout: 30000,
      });

      const blob = new Blob([response.data], {
        type: "text/csv;charset=utf-8;",
      });

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");

      link.href = url;
      link.download =
        searchId !== null
          ? `repusense-busqueda-${searchId}.csv`
          : "repusense-leads.csv";

      document.body.appendChild(link);
      link.click();
      link.remove();

      window.setTimeout(() => {
        window.URL.revokeObjectURL(url);
      }, 1000);

      setNotice(
        searchId !== null
          ? `Se descargó el CSV de la búsqueda #${searchId}.`
          : "El archivo CSV general se descargó correctamente."
      );
    } catch (err) {
      setError(
        `No se pudo exportar el CSV. ${getErrorMessage(err)}`
      );
    } finally {
      if (searchId !== null) {
        setExportingSearchIds((currentIds) => {
          const nextIds = new Set(currentIds);
          nextIds.delete(searchId);
          return nextIds;
        });
      } else {
        setExporting(false);
      }
    }
  };

  const refreshAll = async () => {
    setError("");
    setNotice("");

    await Promise.all([loadLeads(), loadHistory()]);
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

        <div className="topbar-actions">
          <div className={`connection-pill ${connection}`}>
            <span className="connection-dot" />
            {connection === "connected"
              ? "Backend conectado"
              : connection === "disconnected"
                ? "Backend desconectado"
                : "Comprobando conexión"}
          </div>
          <span className="signed-in-user">{user.name}</span>
          <button
            className="button button-secondary logout-button"
            onClick={onLogout}
            type="button"
          >
            <i className="fa-solid fa-arrow-right-from-bracket" />
            Cerrar sesión
          </button>
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
            onClick={refreshAll}
            disabled={loadingLeads || loadingHistory || searching}
            type="button"
          >
            <i
              className={`fa-solid ${
                loadingLeads || loadingHistory
                  ? "fa-spinner fa-spin"
                  : "fa-rotate"
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
                  resultados válidos en PostgreSQL.
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
            La búsqueda utiliza SerpApi para consultar comercios de Google
            Maps. Las calificaciones y reseñas se muestran cuando la fuente
            proporciona esos datos.
          </p>
        </section>

        <section className="panel data-panel">
          <div className="tabs">
            <button
              className={activeTab === "leads" ? "tab active" : "tab"}
              onClick={() => setActiveTab("leads")}
              type="button"
            >
              <i className="fa-solid fa-list-check" />
              Comercios y leads
              <span>{businesses.length}</span>
            </button>

            <button
              className={activeTab === "history" ? "tab active" : "tab"}
              onClick={() => {
                setActiveTab("history");
                loadHistory();
              }}
              type="button"
            >
              <i className="fa-solid fa-clock-rotate-left" />
              Historial
              <span>{history.length}</span>
            </button>

            {user.role === "ADMIN" && (
              <button
                className={activeTab === "admin" ? "tab active" : "tab"}
                onClick={() => setActiveTab("admin")}
                type="button"
              >
                <i className="fa-solid fa-store-gear" />
                ADMINISTRACIÓN
              </button>
            )}
          </div>

          {activeTab === "admin" && user.role === "ADMIN" ? (
            <AdminBusinesses onDataChanged={loadLeads} />
          ) : activeTab === "leads" ? (
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
                    onClick={() => exportCsv()}
                    disabled={exporting}
                    type="button"
                  >
                    <i
                      className={`fa-solid ${
                        exporting ? "fa-spinner fa-spin" : "fa-file-csv"
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
                            {item.risk !== null ? (
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
                                type="button"
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
                                type="button"
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
                                type="button"
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
                  disabled={loadingHistory}
                  type="button"
                >
                  <i
                    className={`fa-solid ${
                      loadingHistory
                        ? "fa-spinner fa-spin"
                        : "fa-rotate"
                    }`}
                  />
                  {loadingHistory ? "Actualizando..." : "Actualizar historial"}
                </button>
              </div>

              {loadingHistory && history.length === 0 ? (
                <div className="empty-state">
                  <i className="fa-solid fa-spinner fa-spin" />
                  <p>Consultando el historial en el backend...</p>
                </div>
              ) : history.length === 0 && historyError ? (
                <div className="notice notice-error">
                  <i className="fa-solid fa-circle-exclamation" />
                  <span>{historyError}</span>
                  <button
                    onClick={loadHistory}
                    type="button"
                    className="button button-secondary"
                  >
                    Reintentar
                  </button>
                </div>
              ) : history.length === 0 ? (
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
                <>
                  {historyError && (
                    <div className="notice notice-error">
                      <i className="fa-solid fa-circle-exclamation" />
                      <span>
                        {historyError} Se muestran los registros cargados
                        anteriormente.
                      </span>
                      <button
                        onClick={loadHistory}
                        type="button"
                        className="button button-secondary"
                      >
                        Reintentar
                      </button>
                    </div>
                  )}

                  {loadingHistory && (
                    <p className="help-text">
                      <i className="fa-solid fa-spinner fa-spin" />
                      Actualizando historial...
                    </p>
                  )}

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
                          <th>Exportar</th>
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
                            <td>
                              <button
                                className="button button-secondary"
                                type="button"
                                onClick={() => exportCsv(item.id)}
                                disabled={
                                  String(item.status || "").toUpperCase() !==
                                    "COMPLETED" ||
                                  Number(item.savedCount ?? 0) <= 0 ||
                                  exportingSearchIds.has(item.id)
                                }
                                title="Descargar los comercios de esta búsqueda"
                              >
                                <i
                                  className={`fa-solid ${
                                    exportingSearchIds.has(item.id)
                                      ? "fa-spinner fa-spin"
                                      : "fa-file-csv"
                                  }`}
                                />
                                {exportingSearchIds.has(item.id)
                                  ? "Exportando..."
                                  : "Descargar CSV"}
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </>
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
                <h3 id="analysis-title">
                  {selectedLead.displayName ||
                    businessOf(selectedLead).name ||
                    `Comercio #${selectedLead.id}`}
                </h3>
              </div>

              <button
                className="icon-button close-button"
                onClick={() => setSelectedLead(null)}
                aria-label="Cerrar"
                type="button"
              >
                <i className="fa-solid fa-xmark" />
              </button>
            </div>

            {selectedLead.analysis ? (
              <div className="analysis-content">
                <p className="analysis-label">
                  <i className="fa-solid fa-wand-magic-sparkles" />
                  Resultado del análisis con Gemini
                </p>

                <div className="notice notice-info">
                  <i className="fa-solid fa-circle-info" />
                  <span>
                    {selectedLead.hasReviews === true
                      ? "Análisis con reseñas reales disponibles."
                      : selectedLead.hasReviews === false
                        ? "Análisis sin reseñas reales. El resultado se basa en la información disponible del comercio."
                        : "No se informó si el análisis utilizó reseñas reales."}
                  </span>
                </div>

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
                  onClick={() => analyzeLead(selectedLead)}
                  disabled={analyzingId === selectedLead.id}
                  type="button"
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