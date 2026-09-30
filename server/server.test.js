const test = require("node:test");
const assert = require("node:assert/strict");
const { spawn } = require("node:child_process");

test("server starts and health endpoint responds", async (t) => {
  const port = 3456;
  const child = spawn(process.execPath, ["server.js"], {
    cwd: __dirname,
    env: { ...process.env, PORT: String(port), HOST: "127.0.0.1" },
    stdio: ["ignore", "pipe", "pipe"]
  });

  t.after(() => child.kill("SIGTERM"));

  await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error("server startup timeout")), 5000);

    child.stdout.on("data", (chunk) => {
      if (chunk.toString().includes("TESTEO SERVER listening")) {
        clearTimeout(timer);
        resolve();
      }
    });

    child.on("error", reject);
    child.on("exit", (code) => {
      if (code !== null && code !== 0) {
        clearTimeout(timer);
        reject(new Error(`server exited with code ${code}`));
      }
    });
  });

  const response = await fetch(`http://127.0.0.1:${port}/api/health`);
  assert.equal(response.status, 200);

  const payload = await response.json();
  assert.equal(payload.ok, true);
  assert.equal(payload.status, "healthy");
  assert.equal(typeof payload.timestamp, "string");
  assert.equal(typeof payload.uptime, "number");
});
