const http = require("node:http");
const os = require("node:os");

const PORT = Number(process.env.PORT || 3000);
const HOST = process.env.HOST || "0.0.0.0";

function json(res, status, data) {
  const body = JSON.stringify(data);
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": Buffer.byteLength(body),
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET,OPTIONS",
    "Cache-Control": "no-store"
  });
  res.end(body);
}

function route(req, res) {
  if (req.method === "OPTIONS") {
    res.writeHead(204, {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET,OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type"
    });
    return res.end();
  }

  if (req.method !== "GET") {
    return json(res, 405, { ok: false, error: "Method not allowed" });
  }

  const url = new URL(req.url, `http://${req.headers.host || "localhost"}`);

  switch (url.pathname) {
    case "/":
      return json(res, 200, {
        ok: true,
        name: "TESTEO SERVER",
        service: "testeo",
        status: "online"
      });

    case "/api/health":
      return json(res, 200, {
        ok: true,
        status: "healthy",
        timestamp: new Date().toISOString(),
        uptime: Math.round(process.uptime()),
        node: process.version
      });

    case "/api/ping":
      return json(res, 200, {
        ok: true,
        pong: true,
        timestamp: Date.now()
      });

    case "/api/status":
      return json(res, 200, {
        ok: true,
        service: "testeo",
        platform: process.platform,
        arch: process.arch,
        hostname: os.hostname(),
        uptime: Math.round(process.uptime())
      });

    default:
      return json(res, 404, {
        ok: false,
        error: "Not found",
        path: url.pathname
      });
  }
}

const server = http.createServer(route);

server.listen(PORT, HOST, () => {
  console.log(`TESTEO SERVER listening on http://${HOST}:${PORT}`);
});

process.on("SIGTERM", () => {
  server.close(() => process.exit(0));
});

process.on("SIGINT", () => {
  server.close(() => process.exit(0));
});
