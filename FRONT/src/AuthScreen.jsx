import { useState } from "react";
import { api, saveSession } from "./apiClient";

function getErrorMessage(error) {
  const data = error.response?.data;
  return (
    data?.message ||
    data?.error ||
    (typeof data === "string" ? data : "") ||
    "No se pudo completar la solicitud. Intentá nuevamente."
  );
}

export default function AuthScreen({
  mode,
  onModeChange,
  onAuthenticated,
}) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const registering = mode === "register";

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setNotice("");

    if (registering && password !== confirmation) {
      setError("Las contraseñas no coinciden.");
      return;
    }

    setLoading(true);
    try {
      if (registering) {
        await api.post("/api/auth/register", {
          name: name.trim(),
          email: email.trim(),
          password,
        });
        setPassword("");
        setConfirmation("");
        setNotice("Cuenta creada. Ya podés iniciar sesión.");
        onModeChange("login");
      } else {
        const response = await api.post("/api/auth/login", {
          email: email.trim(),
          password,
        });
        const expiresAt = saveSession(response.data.token, response.data.user);
        onAuthenticated({
          token: response.data.token,
          user: response.data.user,
          expiresAt,
        });
      }
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  };

  const switchMode = () => {
    setError("");
    setNotice("");
    setPassword("");
    setConfirmation("");
    onModeChange(registering ? "login" : "register");
  };

  return (
    <main className="auth-page">
      <section className="auth-card" aria-labelledby="auth-title">
        <div className="brand auth-brand">
          <div className="brand-mark">
            <i className="fa-solid fa-shield-halved" />
          </div>
          <div>
            <h1>RepuSense</h1>
            <p>Radar de reputación y prospección inteligente con IA</p>
          </div>
        </div>

        <div className="auth-heading">
          <p className="eyebrow">
            {registering ? "CREÁ TU CUENTA" : "ACCESO SEGURO"}
          </p>
          <h2 id="auth-title">
            {registering ? "Crear cuenta" : "Bienvenido nuevamente"}
          </h2>
          <p className="muted">
            {registering
              ? "Registrate para acceder a tu espacio de prospección."
              : "Iniciá sesión para continuar al panel de RepuSense."}
          </p>
        </div>

        {error && (
          <div className="notice notice-error" role="alert">
            <i className="fa-solid fa-circle-exclamation" />
            <span>{error}</span>
          </div>
        )}

        {notice && (
          <div className="notice notice-success" role="status">
            <i className="fa-solid fa-circle-check" />
            <span>{notice}</span>
          </div>
        )}

        <form className="auth-form" onSubmit={handleSubmit}>
          {registering && (
            <label>
              Nombre
              <input
                autoComplete="name"
                maxLength={100}
                onChange={(event) => setName(event.target.value)}
                required
                value={name}
              />
            </label>
          )}

          <label>
            Correo electrónico
            <input
              autoComplete="email"
              onChange={(event) => setEmail(event.target.value)}
              required
              type="email"
              value={email}
            />
          </label>

          <label>
            Contraseña
            <span className="auth-password-field">
              <input
                autoComplete={registering ? "new-password" : "current-password"}
                minLength={registering ? 8 : undefined}
                onChange={(event) => setPassword(event.target.value)}
                required
                type={showPassword ? "text" : "password"}
                value={password}
              />
              <button
                aria-label={showPassword ? "Ocultar contraseña" : "Mostrar contraseña"}
                className="auth-password-toggle"
                onClick={() => setShowPassword((visible) => !visible)}
                type="button"
              >
                <i className={`fa-solid ${showPassword ? "fa-eye-slash" : "fa-eye"}`} />
              </button>
            </span>
          </label>

          {registering && (
            <label>
              Confirmar contraseña
              <span className="auth-password-field">
                <input
                  autoComplete="new-password"
                  minLength={8}
                  onChange={(event) => setConfirmation(event.target.value)}
                  required
                  type={showConfirmation ? "text" : "password"}
                  value={confirmation}
                />
                <button
                  aria-label={
                    showConfirmation
                      ? "Ocultar confirmación"
                      : "Mostrar confirmación"
                  }
                  className="auth-password-toggle"
                  onClick={() =>
                    setShowConfirmation((visible) => !visible)
                  }
                  type="button"
                >
                  <i
                    className={`fa-solid ${
                      showConfirmation ? "fa-eye-slash" : "fa-eye"
                    }`}
                  />
                </button>
              </span>
            </label>
          )}

          <button
            className="button button-primary auth-submit"
            disabled={loading}
            type="submit"
          >
            <i
              className={`fa-solid ${
                loading ? "fa-spinner fa-spin" : "fa-arrow-right-to-bracket"
              }`}
            />
            {loading
              ? "Procesando..."
              : registering
                ? "Crear cuenta"
                : "Iniciar sesión"}
          </button>
        </form>

        <p className="auth-switch">
          {registering ? "¿Ya tenés una cuenta?" : "¿Todavía no tenés cuenta?"}{" "}
          <button onClick={switchMode} type="button">
            {registering ? "Iniciar sesión" : "Crear una cuenta"}
          </button>
        </p>
      </section>
    </main>
  );
}
