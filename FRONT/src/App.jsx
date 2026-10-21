import React, { useState, useEffect } from 'react';
import axios from 'axios';

export default function App() {
  const [comercios, setComercios] = useState([]);
  const [loading, setLoading] = useState(false);
  const [dbaConectado, setDbaConectado] = useState(true);
  const [filtroUbicacion, setFiltroUbicacion] = useState('todos');
  const [filtroRiesgo, setFiltroRiesgo] = useState('todos');
  const [filtroProblema, setFiltroProblema] = useState('todos');
  const [alerta, setAlerta] = useState(null);
  const [enviando, setEnviando] = useState(false);

  // URL de tu Backend Spring Boot
  const BACKEND_URL = "http://localhost:8080/api/v1";

  // Función para mostrar alertas temporales
  const showAlert = (mensaje, tipo = "info") => {
    setAlerta({ mensaje, tipo });
    setTimeout(() => setAlerta(null), 5000);
  };

  // 1. Conexión y Diagnóstico al presionar el botón
  const ejecutarDiagnostico = async () => {
    setLoading(true);
    setAlerta(null);

    try {
      // 1. Petición al Backend para obtener Leads o Comercios analizados
      const res = await axios.get(`${BACKEND_URL}/leads`);

      // Adaptar respuesta (Spring Boot devuelve estructura Page { content: [...] } o array)
      const data = res.data.content ? res.data.content : res.data;

      const adaptados = data.map((item, idx) => {
        const pLower = (item.aiMainProblem || "").toLowerCase();
        let problema = "atencion";
        let riesgo = 4;

        if (pLower.includes("higiene") || pLower.includes("sanitar") || pLower.includes("limpiez")) {
          problema = "higiene";
          riesgo = 5;
        } else if (pLower.includes("espera") || pLower.includes("demora") || pLower.includes("tardo")) {
          problema = "demoras";
          riesgo = 3;
        } else if (pLower.includes("tarifa") || pLower.includes("precio") || pLower.includes("cobro")) {
          problema = "precios";
          riesgo = 4;
        } else if (pLower.includes("atenci") || pLower.includes("trato")) {
          problema = "atencion";
          riesgo = 4;
        }

        return {
          id: item.id || idx + 1,
          negocio: item.business?.name || item.name || "Comercio",
          email: item.contactEmail || "contacto@comercio.com",
          ubicacion: item.business?.city || "Buenos Aires, Argentina",
          rubro: item.business?.category || "Gastronomía",
          riesgo,
          problema,
          resumen: item.aiEmailOpening || "Diagnóstico de reputación en proceso.",
          status: item.status === "CONTACTED" ? "Contactado" : "No contactado",
          seleccionado: false
        };
      });

      setComercios(adaptados);
      setDbaConectado(true);
      showAlert(`Diagnóstico completado. Se cargaron ${adaptados.length} comercios analizados.`, "success");
    } catch (error) {
      console.error("Error conectando al backend:", error);
      setDbaConectado(false);
      showAlert("❌ Error de conexión: No se pudo conectar con el Backend (localhost:8080) ni PostgreSQL.", "error");
    } finally {
      setLoading(false);
    }
  };

  // Manejo de Selección de Checkbox
  const toggleSeleccion = (id) => {
    setComercios(prev => prev.map(c => c.id === id ? { ...c, seleccionado: !c.seleccionado } : c));
  };

  const seleccionados = comercios.filter(c => c.seleccionado);

  // Enviar correos y actualizar estado a CONTACTED en backend y frontend
  const enviarCorreos = async () => {
    if (seleccionados.length === 0) return;
    setEnviando(true);
    try {
      await Promise.all(
        seleccionados.map(s =>
          axios.patch(`${BACKEND_URL}/leads/${s.id}/status`, { status: "CONTACTED" })
        )
      );
      setComercios(prev =>
        prev.map(c => (c.seleccionado ? { ...c, status: "Contactado", seleccionado: false } : c))
      );
      showAlert(`¡Correos enviados con éxito a ${seleccionados.length} comercios! Estado actualizado a Contactado.`, "success");
    } catch (error) {
      console.error("Error al actualizar estado:", error);
      showAlert("⚠️ No se pudo actualizar el estado de los correos en la base de datos.", "error");
    } finally {
      setEnviando(false);
    }
  };

  // Filtrado reactivo en pantalla
  const comerciosFiltrados = comercios.filter(c => {
    if (filtroUbicacion !== 'todos' && !c.ubicacion.toLowerCase().includes(filtroUbicacion)) return false;
    if (filtroRiesgo !== 'todos' && c.riesgo !== parseInt(filtroRiesgo)) return false;
    if (filtroProblema !== 'todos' && c.problema !== filtroProblema) return false;
    return true;
  });

  return (
    <div className="bg-slate-900 text-slate-100 min-h-screen flex flex-col">
      {/* Header */}
      <header className="border-b border-slate-800 bg-slate-900/90 sticky top-0 z-50 backdrop-blur-md">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-500/20">
              <i className="fa-solid fa-shield-halved text-white text-lg"></i>
            </div>
            <div>
              <h1 className="font-bold text-lg text-white">REPUSENSE - Dashboard</h1>
              <p className="text-xs text-slate-400">Radar de reputación y prospección inteligente con IA</p>
            </div>
          </div>
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
            <span className={`w-2.5 h-2.5 rounded-full ${dbaConectado ? 'bg-emerald-500 animate-ping' : 'bg-rose-500'}`}></span>
            <span>{dbaConectado ? 'DBA Conectado en Vivo' : 'DBA Desconectado'}</span>
          </div>
        </div>
      </header>

      {/* Alerta Global */}
      {alerta && (
        <div className="max-w-7xl mx-auto px-4 mt-4 w-full">
          <div className={`p-4 rounded-xl flex items-center justify-between border text-sm ${alerta.tipo === 'error' ? 'bg-rose-500/10 border-rose-500/30 text-rose-300' : 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
            }`}>
            <span>{alerta.mensaje}</span>
            <button onClick={() => setAlerta(null)} className="text-slate-400 hover:text-white"><i className="fa-solid fa-xmark"></i></button>
          </div>
        </div>
      )}

      {/* Main Container */}
      <main className="max-w-7xl mx-auto px-4 py-6 flex-1 w-full space-y-6">
        {/* Panel de Filtros y Acción */}
        <div className="bg-slate-800/70 border border-slate-700/80 rounded-2xl p-6 shadow-xl space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="text-xs text-slate-300">Ubicación</label>
              <select value={filtroUbicacion} onChange={e => setFiltroUbicacion(e.target.value)} className="w-full mt-1 bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white">
                <option value="todos">Todas las ubicaciones</option>
                <option value="buenos aires">Buenos Aires</option>
                <option value="cordoba">Córdoba</option>
                <option value="rosario">Rosario</option>
              </select>
            </div>
            <div>
              <label className="text-xs text-slate-300">Scoring / Riesgo</label>
              <select value={filtroRiesgo} onChange={e => setFiltroRiesgo(e.target.value)} className="w-full mt-1 bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white">
                <option value="todos">Todos los niveles</option>
                <option value="5">Nivel 5 (Crítico)</option>
                <option value="4">Nivel 4 (Alto)</option>
              </select>
            </div>
            <div>
              <label className="text-xs text-slate-300">Problema Raíz</label>
              <select value={filtroProblema} onChange={e => setFiltroProblema(e.target.value)} className="w-full mt-1 bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white">
                <option value="todos">Cualquier problema</option>
                <option value="higiene">Higiene</option>
                <option value="atencion">Atención</option>
                <option value="demoras">Demoras</option>
                <option value="precios">Precios / Tarifas</option>
              </select>
            </div>
          </div>

          <div className="flex justify-end pt-2">
            <button
              onClick={ejecutarDiagnostico}
              disabled={loading}
              className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-semibold px-6 py-2.5 rounded-xl shadow-lg shadow-blue-500/25 flex items-center gap-2 text-sm disabled:opacity-50"
            >
              <i className={`fa-solid ${loading ? 'fa-circle-notch fa-spin' : 'fa-wand-magic-sparkles'}`}></i>
              <span>{loading ? "Ejecutando diagnóstico..." : "Comenzar Diagnóstico"}</span>
            </button>
          </div>
        </div>

        {/* Vista de Carga Doble (DBA + IA) */}
        {loading && (
          <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-10 text-center space-y-4">
            <div className="flex items-center justify-center gap-8">
              <div className="flex flex-col items-center">
                <i className="fa-solid fa-database text-3xl text-blue-400 animate-pulse"></i>
                <span className="text-xs text-amber-400 mt-2 font-medium">1. Consultando PostgreSQL...</span>
              </div>
              <i className="fa-solid fa-arrow-right text-slate-600 text-xl"></i>
              <div className="flex flex-col items-center">
                <i className="fa-solid fa-brain text-3xl text-indigo-400 animate-pulse"></i>
                <span className="text-xs text-indigo-400 mt-2 font-medium">2. Evaluando reseñas con IA...</span>
              </div>
            </div>
          </div>
        )}

        {/* Layout Principal: Tabla + Panel Lateral de IA */}
        {!loading && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            <div className={`${seleccionados.length > 0 ? 'lg:col-span-7' : 'lg:col-span-12'} bg-slate-800/70 border border-slate-700/80 rounded-2xl p-6 shadow-xl`}>
              <h2 className="text-base font-bold text-white mb-4">
                Comercios Identificados ({comerciosFiltrados.length})
              </h2>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-900/50 text-slate-400 uppercase">
                    <tr>
                      <th className="p-3 text-center">Sel.</th>
                      <th className="p-3">Estado</th>
                      <th className="p-3">Comercio</th>
                      <th className="p-3">Ubicación</th>
                      <th className="p-3 text-center">Riesgo</th>
                      <th className="p-3">Problema</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-700/50">
                    {comerciosFiltrados.map((item) => (
                      <tr key={item.id} className="hover:bg-slate-800/40">
                        <td className="p-3 text-center">
                          <input
                            type="checkbox"
                            checked={item.seleccionado}
                            onChange={() => toggleSeleccion(item.id)}
                            className="rounded bg-slate-900 text-blue-600 cursor-pointer"
                          />
                        </td>
                        <td className="p-3">
                          <span className={`px-2 py-0.5 rounded-full text-[10px] ${item.status === 'Contactado' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-amber-500/10 text-amber-400'
                            }`}>
                            {item.status}
                          </span>
                        </td>
                        <td className="p-3 font-semibold text-white">{item.negocio}</td>
                        <td className="p-3 text-slate-300">{item.ubicacion}</td>
                        <td className="p-3 text-center">
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${item.riesgo >= 5 ? 'bg-rose-600 text-white' : 'bg-amber-500 text-slate-900'
                            }`}>
                            {item.riesgo}
                          </span>
                        </td>
                        <td className="p-3 capitalize">{item.problema}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Panel Lateral Desplegable de IA */}
            {seleccionados.length > 0 && (
              <div className="lg:col-span-5 bg-slate-800/90 border border-slate-700/80 rounded-2xl p-5 space-y-4">
                <h3 className="text-sm font-bold text-white flex items-center justify-between">
                  <span>Emails con IA ({seleccionados.length})</span>
                  <span className="text-xs text-blue-400 font-mono">Borradores Listos</span>
                </h3>

                <div className="space-y-3 max-h-[400px] overflow-y-auto">
                  {seleccionados.map(s => (
                    <div key={s.id} className="bg-slate-900/80 p-3 rounded-xl border border-slate-700/80 space-y-1">
                      <p className="text-xs font-bold text-white">{s.negocio}</p>
                      <p className="text-[11px] text-slate-300 italic bg-slate-800/50 p-2 rounded">
                        "{s.resumen}"
                      </p>
                    </div>
                  ))}
                </div>

                <button
                  onClick={enviarCorreos}
                  disabled={enviando}
                  className="w-full bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white font-semibold py-2.5 rounded-xl text-xs transition flex items-center justify-center gap-2 cursor-pointer"
                >
                  <i className={`fa-solid ${enviando ? "fa-circle-notch fa-spin" : "fa-paper-plane"}`}></i>
                  <span>{enviando ? "Enviando y actualizando estado..." : "Enviar Correos con IA"}</span>
                </button>
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  );
}
