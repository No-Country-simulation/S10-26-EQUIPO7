# RepuSense Backend

## Primer administrador

El registro público siempre crea usuarios con el rol `USER`. Para crear o promover
un administrador de forma explícita, configure estas variables de entorno antes
de iniciar Spring Boot:

- `APP_ADMIN_BOOTSTRAP_EMAIL`: correo normalizado de la cuenta administradora.
- `APP_ADMIN_BOOTSTRAP_PASSWORD`: contraseña de al menos 12 caracteres.

Si el correo todavía no existe, el arranque crea la cuenta y almacena la contraseña
con BCrypt. Si ya existe, solo la promueve cuando la contraseña configurada coincide
con la contraseña de esa cuenta; una discrepancia no cambia el rol y detiene el
arranque. Las cuentas inactivas no se reactivan. Una vez iniciado correctamente, se
pueden retirar ambas variables: el rol queda persistido en PostgreSQL. No agregue
estos valores a archivos versionados.

## API de administración

Los siguientes endpoints requieren un JWT de un usuario `ADMIN`:

- `GET /api/admin/businesses`: listado administrativo, con ciudad, rubro, dirección,
  calificación, reseñas y estado del lead.
- `DELETE /api/admin/businesses/{id}`: elimina un comercio individual.
- `DELETE /api/admin/businesses/bulk`: recibe `{"ids":[1,2]}` y valida toda la
  selección antes de eliminar.

Las bajas eliminan el lead y las reseñas asociadas al comercio, y quitan las
asociaciones del comercio con búsquedas anteriores. No se eliminan registros
`search_run` ni se alteran sus conteos históricos. Las operaciones son transaccionales.
La baja heredada `DELETE /api/v1/businesses/{id}` también requiere ADMIN.

## Swagger

Con el backend iniciado, abra `http://localhost:8080/swagger-ui/index.html`.
Inicie sesión por `POST /api/auth/login` con una cuenta ADMIN, copie el campo
`token` de la respuesta, pulse **Authorize** y pegue el JWT (sin el prefijo `Bearer`).
La API de administración devuelve `403` para `USER` y `401` si falta un token válido.
