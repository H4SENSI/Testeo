const test = require("node:test");
const assert = require("node:assert/strict");

test("health payload contract", () => {
  const payload = {
    ok: true,
    status: "healthy"
  };

  assert.equal(payload.ok, true);
  assert.equal(payload.status, "healthy");
});
