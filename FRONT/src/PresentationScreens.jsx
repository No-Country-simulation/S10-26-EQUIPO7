export function WelcomeScreen() {
  return (
    <main className="presentation-page welcome-page">
      <div className="presentation-glow" aria-hidden="true" />
      <section className="presentation-content" aria-labelledby="welcome-title">
        <div className="presentation-brand">
          <span className="presentation-brand-mark" aria-hidden="true">
            <i className="fa-solid fa-shield-halved" />
          </span>
          <span>REPUSENSE</span>
        </div>

        <p className="presentation-eyebrow">
          <span />
          INTELIGENCIA · REPUTACIÓN · OPORTUNIDADES
        </p>
        <h1 id="welcome-title">Radar de reputación online</h1>
        <p className="presentation-description">
          Plataforma inteligente para identificar problemas de reputación y
          generar oportunidades comerciales.
        </p>

        <a className="presentation-cta" href="/login">
          Ingresar a la plataforma
          <i className="fa-solid fa-arrow-right" aria-hidden="true" />
        </a>

        <div className="presentation-visual" aria-hidden="true">
          <div className="visual-orbit orbit-outer" />
          <div className="visual-orbit orbit-inner" />
          <div className="visual-core">
            <i className="fa-solid fa-chart-line" />
          </div>
          <span className="visual-point point-one" />
          <span className="visual-point point-two" />
          <span className="visual-point point-three" />
          <div className="visual-label label-top">ANÁLISIS EN TIEMPO REAL</div>
          <div className="visual-label label-bottom">
            <span />
            DATOS QUE IMPULSAN
          </div>
        </div>
      </section>

      <footer className="presentation-footer">
        <span>REPUTACIÓN DIGITAL, CON VISIÓN ESTRATÉGICA</span>
        <span className="footer-indicator">
          <span />
          SISTEMA ACTIVO
        </span>
      </footer>
    </main>
  );
}

export function ThankYouScreen() {
  return (
    <main className="presentation-page thank-you-page">
      <div className="presentation-glow" aria-hidden="true" />
      <section
        className="presentation-content thank-you-content"
        aria-labelledby="thank-you-title"
      >
        <div className="presentation-brand">
          <span className="presentation-brand-mark" aria-hidden="true">
            <i className="fa-solid fa-shield-halved" />
          </span>
          <span>REPUSENSE</span>
        </div>

        <div className="thank-you-icon" aria-hidden="true">
          <i className="fa-solid fa-circle-check" />
        </div>
        <p className="presentation-eyebrow">
          <span />
          CADA DATO ABRE UN CAMINO
        </p>
        <h1 id="thank-you-title">¡Gracias por visitarnos!</h1>
        <p className="presentation-description">
          Transformamos información en oportunidades comerciales
        </p>
        <p className="thank-you-signoff">¡Hasta pronto!</p>
      </section>

      <footer className="presentation-footer">
        <span>REPUSENSE · RADAR DE REPUTACIÓN ONLINE</span>
        <span className="footer-indicator">
          <span />
          FIN DE LA DEMOSTRACIÓN
        </span>
      </footer>
    </main>
  );
}
