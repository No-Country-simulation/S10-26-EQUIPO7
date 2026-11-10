# 🚀 RepuSense - Frontend

Frontend de **RepuSense – Radar de Reputación en Crisis**, una aplicación web que permite buscar comercios, identificar oportunidades comerciales y analizar su reputación online mediante inteligencia artificial.

## 🧰 Tecnologías utilizadas

- React
- Vite
- JavaScript
- HTML5
- CSS3
- Axios (comunicación con el backend)
- React Router (navegación, si está implementado en el proyecto)

## ✨ Funcionalidades principales

- **Autenticación:** registro e inicio de sesión de usuarios.
- **Panel principal:** visualización de información y actividad del sistema.
- **Búsqueda de comercios:** búsqueda por ciudad y rubro.
- **Gestión de leads:** consulta, filtrado y seguimiento de prospectos.
- **Análisis con IA:** visualización de diagnósticos y recomendaciones generadas mediante Google Gemini.
- **Historial de búsquedas:** consulta de las búsquedas realizadas.
- **Exportación CSV:** descarga de información de los leads.
- **Administración:** gestión de comercios y funcionalidades según los permisos del usuario.

## 📋 Requisitos previos

- Node.js.
- npm.
- Backend de RepuSense en funcionamiento.
- PostgreSQL configurado desde el backend.

## ⚙️ Instalación

Clonar el repositorio:

```bash
git clone URL_DEL_REPOSITORIO
```

Ingresar a la carpeta del frontend:

```bash
cd FRONT
```

Instalar dependencias:

```bash
npm install
```

## ▶️ Ejecución en desarrollo

Iniciar el servidor de desarrollo:

```bash
npm run dev
```

Por defecto, Vite utiliza:

```text
http://localhost:5173
```

## 🔗 Comunicación con el backend

El frontend consume los servicios REST expuestos por el backend Spring Boot.

URL local del backend:

```text
http://localhost:8080
```

Las solicitudes permiten gestionar usuarios, búsquedas, comercios y leads, además de acceder a los resultados de los análisis realizados con inteligencia artificial.

## 🏗️ Estructura del proyecto

La estructura se organiza en componentes, páginas, servicios y recursos del frontend, según la implementación actual.

## 🧪 Compilación para producción

Generar la versión de producción:

```bash
npm run build
```

Previsualizar la compilación:

```bash
npm run preview
```

## 👥 Proyecto

**RepuSense – Radar de Reputación en Crisis**

Desarrollado en el marco de la simulación laboral de No Country, equipo S10-26-EQUIPO7.