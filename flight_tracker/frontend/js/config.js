/* Global configuration and small helpers shared across the app. */

const CONFIG = {
  // How often to poll the backend for fresh positions (ms).
  refreshInterval: 10000,
  // Don't request a global feed when zoomed out past this level; instead
  // fetch only the current viewport to keep responses small and fast.
  viewportFetchMinZoom: 3,
  // Maximum number of trail points kept per aircraft.
  maxTrailPoints: 30,
  // Initial map view (roughly whole-world, centred on Europe/Atlantic).
  initialCenter: [30, 10],
  initialZoom: 3,
  // Meters -> feet, m/s -> knots.
  M_TO_FT: 3.28084,
  MS_TO_KTS: 1.94384,
  MS_TO_FPM: 196.850394,
};

/* Altitude-based colour scale (metres), matching the legend in the sidebar. */
const ALT_STOPS = [
  { m: 0, color: [228, 11, 11] }, // red — ground / very low
  { m: 1500, color: [245, 166, 35] }, // orange
  { m: 3500, color: [248, 231, 28] }, // yellow
  { m: 6000, color: [126, 211, 33] }, // green
  { m: 9000, color: [74, 144, 226] }, // blue
  { m: 12000, color: [144, 19, 254] }, // violet — very high
];

function altitudeColor(altMeters) {
  if (altMeters == null || Number.isNaN(altMeters)) return "#9aa4b8";
  const a = Math.max(0, altMeters);
  for (let i = 0; i < ALT_STOPS.length - 1; i++) {
    const lo = ALT_STOPS[i];
    const hi = ALT_STOPS[i + 1];
    if (a <= hi.m) {
      const t = (a - lo.m) / (hi.m - lo.m || 1);
      const c = lo.color.map((v, j) => Math.round(v + (hi.color[j] - v) * t));
      return `rgb(${c[0]}, ${c[1]}, ${c[2]})`;
    }
  }
  const last = ALT_STOPS[ALT_STOPS.length - 1].color;
  return `rgb(${last[0]}, ${last[1]}, ${last[2]})`;
}

/* Formatting helpers -------------------------------------------------------- */

function fmtAltitude(m) {
  if (m == null) return "—";
  return `${Math.round(m * CONFIG.M_TO_FT).toLocaleString()} ft`;
}

function fmtSpeed(ms) {
  if (ms == null) return "—";
  return `${Math.round(ms * CONFIG.MS_TO_KTS)} kts`;
}

function fmtVerticalRate(ms) {
  if (ms == null) return "—";
  const fpm = Math.round(ms * CONFIG.MS_TO_FPM);
  const arrow = fpm > 50 ? " ↑" : fpm < -50 ? " ↓" : "";
  return `${fpm.toLocaleString()} ft/min${arrow}`;
}

function fmtHeading(deg) {
  if (deg == null) return "—";
  const dirs = ["N", "NE", "E", "SE", "S", "SW", "W", "NW"];
  const dir = dirs[Math.round(deg / 45) % 8];
  return `${Math.round(deg)}° ${dir}`;
}

function fmtCoord(lat, lon) {
  if (lat == null || lon == null) return "—";
  const ns = lat >= 0 ? "N" : "S";
  const ew = lon >= 0 ? "E" : "W";
  return `${Math.abs(lat).toFixed(3)}°${ns}, ${Math.abs(lon).toFixed(3)}°${ew}`;
}
