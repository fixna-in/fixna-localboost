import { readFileSync } from "node:fs";

const file = process.argv[2];
const raw = readFileSync(file, "utf8");
let doc;
try {
  doc = JSON.parse(raw);
} catch (e) {
  console.error("JSON_INVALID: " + e.message);
  process.exit(2);
}
console.log("JSON_VALID");
console.log("schema=" + doc.info.schema);
console.log("name=" + doc.info.name);
console.log("bytes=" + raw.length);
console.log("folders=" + doc.item.length);
console.log("folders: " + doc.item.map((f) => f.name).join(" | "));
let total = 0;
const walk = (items, depth) => {
  for (const it of items) {
    if (it.item) {
      walk(it.item, depth + 1);
    } else {
      total += 1;
      const u = it.request && it.request.url ? it.request.url.raw : "(no url)";
      const m = it.request && it.request.method ? it.request.method : "?";
      console.log(`${"  ".repeat(depth)}${m} ${u}`);
    }
  }
};
walk(doc.item, 1);
console.log("requests=" + total);
const vars = (doc.variable || []).map((v) => v.key);
console.log("variables=" + vars.join(","));