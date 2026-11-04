import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// The Play app serves the build from its public/ folder, so that is where it goes.
// In development, run Play on 9000 (sbt run) and this on 5173; /api is passed through.
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: "../public",
    emptyOutDir: true,
    rollupOptions: {
      // The app, and the small page other sites embed (served at /embed/:id).
      input: { main: "index.html", embed: "embed.html" },
    },
  },
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": `http://localhost:${process.env.API_PORT ?? 9000}`,
    },
  },
});
