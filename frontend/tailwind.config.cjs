/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        surface: "#f6f7fb",
        ink: "#0f172a",
        primary: "#0d9488",
        accent: "#f59e0b",
      },
      boxShadow: {
        panel: "0 10px 25px rgba(15, 23, 42, 0.08)",
      },
    },
  },
  plugins: [],
};
