import { readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";

const root = process.argv[2];
const files = [];
const walk = (dir) => {
  for (const entry of readdirSync(dir)) {
    const p = join(dir, entry);
    if (statSync(p).isDirectory()) walk(p);
    else if (entry.endsWith("Controller.java")) files.push(p);
  }
};
walk(root);

const mapping = /@(Get|Post|Put|Patch|Delete)Mapping\s*(?:\(\s*(?:value\s*=\s*)?((?:"[^"]*"|\{[^}]*\}))?)?/g;

for (const file of files.sort()) {
  const src = readFileSync(file, "utf8");
  const rel = file.replace(root, "").replace(/\\/g, "/");
  const classBase = src.match(/@RequestMapping\s*\(\s*((?:"[^"]*")|(?:\{[^}]*\}))/);
  const base = classBase ? classBase[1].replace(/"/g, "") : "";
  console.log("\n### " + rel + "  base=" + (base || "(none)"));
  const lines = src.split(/\r?\n/);
  lines.forEach((line, idx) => {
    mapping.lastIndex = 0;
    const m = mapping.exec(line);
    if (!m) return;
    const verb = m[1].toUpperCase();
    const path = m[2] ? m[2].replace(/"/g, "").replace(/[{}]/g, "") : "";
    // look ahead for the method signature + preceding annotations
    let sig = "";
    let security = "";
    for (let i = idx; i < Math.min(idx + 6, lines.length); i++) {
      const l = lines[i].trim();
      if (/PreAuthorize|RolesAllowed|Secured/.test(l)) security += l + " ";
      if (/public\s+[A-Za-z]/.test(l) && !sig) sig = l.slice(0, 160);
    }
    console.log(`  ${verb} ${base}${path}   :: ${sig}${security ? "  [" + security + "]" : ""}`);
  });
}