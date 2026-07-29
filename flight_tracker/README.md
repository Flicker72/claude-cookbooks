# Worldwide Flight Tracker

A self-contained, Flightradar-style live flight tracker. It shows real
aircraft positions worldwide on a dark world map, sourced from the
[OpenSky Network](https://opensky-network.org) live ADS-B feed.

![Flight tracker](https://img.shields.io/badge/data-OpenSky%20Network-4a90e2)
![Python](https://img.shields.io/badge/backend-stdlib%20only-7ed321)

## Features

- 🌍 **Live worldwide flights** — real ADS-B positions, refreshed every ~10s
- ✈ **Heading-rotated aircraft icons**, colour-coded by altitude
- 🖱 **Click any aircraft** for airline, callsign, altitude, speed, heading,
  vertical rate, position, ICAO24, squawk and status
- 🏷 **Airline identification** — decodes the ICAO operator prefix in each
  callsign (e.g. `BAW123` → British Airways)
- 🛫 **Major airports layer** — a curated set of ~75 world hubs, toggleable,
  with hover tooltips
- 🧭 **Follow mode** — keep the map centred on a chosen flight
- 🛰 **Flight trails** built client-side as aircraft move
- 🔎 **Search** by callsign, airline, country, or ICAO24 address
- 🎚 **Filters** — minimum altitude, hide ground traffic, toggle trails,
  toggle airports, pause auto-refresh
- ⚡ **Viewport-aware fetching** — only requests aircraft in the current
  map view when zoomed in, keeping responses small and fast
- 🔌 **Zero dependencies** — the backend uses only the Python standard
  library; the frontend loads Leaflet from a CDN

## Quick start

```bash
cd flight_tracker
python server.py
# then open http://localhost:8000
```

That's it — no API key required. The backend proxies OpenSky, caches
responses to respect rate limits, and shares one upstream fetch across
all connected browsers.

### Custom port

```bash
PORT=9000 python server.py
```

## Higher rate limits (optional)

Anonymous OpenSky access is rate limited. For heavier use, create free
OAuth2 API client credentials in your OpenSky account and export them:

```bash
export OPENSKY_CLIENT_ID=your-client-id
export OPENSKY_CLIENT_SECRET=your-client-secret
python server.py
```

The server automatically fetches and refreshes an access token and falls
back to anonymous access if the credentials are missing or invalid.

## How it works

```
Browser (Leaflet map, js/*.js)
   │  GET /api/states?lamin&lomin&lamax&lomax
   ▼
server.py  ──►  caches (≈8s)  ──►  OpenSky /api/states/all
   │  serves static frontend + JSON proxy
```

- **`server.py`** — a threaded stdlib HTTP server. Serves the frontend,
  proxies `/api/states` (transforming OpenSky's positional arrays into
  named-field JSON), caches results per-viewport, and optionally
  authenticates via OAuth2.
- **`frontend/`** — the single-page app: `config.js` (constants, altitude
  colour scale, formatters), `airlines.js` (ICAO operator-code lookup),
  `airports.js` (major-airport dataset), `api.js` (backend client), and
  `app.js` (map, markers, trails, airports, search, filters, detail panel).

## Configuration

| Env var                 | Default | Purpose                                  |
| ----------------------- | ------- | ---------------------------------------- |
| `PORT`                  | `8000`  | HTTP port                                |
| `HOST`                  | `0.0.0.0` | Bind address                           |
| `CACHE_TTL_SECONDS`     | `8`     | How long upstream responses are reused   |
| `OPENSKY_CLIENT_ID`     | —       | OpenSky OAuth2 client id (optional)      |
| `OPENSKY_CLIENT_SECRET` | —       | OpenSky OAuth2 client secret (optional)  |
| `VERBOSE`               | —       | Set to log every HTTP request            |

## Notes & limitations

- OpenSky's public feed is best-effort: coverage is densest over Europe and
  North America, and some aircraft report no altitude or callsign.
- Trails are accumulated from observed positions while the app is open
  (OpenSky's historical track endpoint is not used), so they grow over time
  rather than showing the full prior flight path.
- This is a demonstration project, not a certified navigation tool.

## Data attribution

Flight data © the [OpenSky Network](https://opensky-network.org). Map tiles
© [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors and
© [CARTO](https://carto.com/attributions).
