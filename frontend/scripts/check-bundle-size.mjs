import fs from "node:fs";
import path from "node:path";
import zlib from "node:zlib";

const distDir = path.resolve("dist");
const manifestPath = path.join(distDir, ".vite", "manifest.json");

if (!fs.existsSync(manifestPath)) {
  console.error("Manifest not found:", manifestPath);
  process.exit(1);
}

const manifest = JSON.parse(fs.readFileSync(manifestPath, "utf-8"));
let maxEntryGzip = 200 * 1024; // 200 KB
let maxChunkGzip = 120 * 1024; // 120 KB

let hasFailure = false;

for (const [key, item] of Object.entries(manifest)) {
  if (item.file && item.file.endsWith(".js")) {
    const filePath = path.join(distDir, item.file);
    if (fs.existsSync(filePath)) {
      const content = fs.readFileSync(filePath);
      const gzipped = zlib.gzipSync(content);
      const sizeKb = (gzipped.length / 1024).toFixed(2);

      const limit = item.isEntry ? maxEntryGzip : maxChunkGzip;
      const limitKb = (limit / 1024).toFixed(0);

      console.log(`${item.file}: ${sizeKb} KB (limit: ${limitKb} KB)`);

      if (gzipped.length > limit) {
        console.error(`FAILED: ${item.file} exceeded limit (${sizeKb} KB > ${limitKb} KB)`);
        hasFailure = true;
      }
    }
  }
}

if (hasFailure) {
  process.exit(1);
} else {
  console.log("All bundle budgets passed!");
}
