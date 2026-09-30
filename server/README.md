# TESTEO Server

Backend HTTP sin dependencias externas.

## Ejecutar localmente

```bash
cd server
npm start
```

Por defecto escucha en el puerto `3000`.

Endpoints:

- `GET /`
- `GET /api/health`
- `GET /api/ping`
- `GET /api/status`

Variables opcionales:

- `PORT`
- `HOST`

GitHub Actions ejecuta el test automáticamente cuando cambia `server/**`.
