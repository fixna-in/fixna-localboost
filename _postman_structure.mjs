import { readFileSync } from "node:fs";
const file = process.argv[2];
const raw = readFileSync(file, "utf8");
const lines = raw.split(/\r?\n/);
console.log("lines=" + lines.length);
lines.forEach((l, i) => {
  if (l.includes("__SENTINEL__")) console.log("SENTINEL at line " + (i + 1));
});
lines.forEach((l, i) => {
  const m = l.match(/^\s{0,10}"name":\s*"([^"]*)"/);
  if (m) console.log((i + 1) + "  " + l.trim().slice(0, 100));
});