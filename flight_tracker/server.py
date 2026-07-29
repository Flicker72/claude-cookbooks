#!/usr/bin/env python3
"""Worldwide flight tracker backend.

A dependency-free HTTP server (Python standard library only) that:

  * serves the static frontend in ``frontend/``
  * proxies the OpenSky Network live states API at ``/api/states``
  * caches upstream responses briefly to respect OpenSky rate limits
  * optionally authenticates with OpenSky OAuth2 client credentials for
    higher rate limits (set ``OPENSKY_CLIENT_ID`` / ``OPENSKY_CLIENT_SECRET``)

Run it with::

    python server.py            # http://localhost:8000
    PORT=9000 python server.py  # custom port

No API key is required for anonymous access, but anonymous requests are
rate limited by OpenSky, so the server caches results and shares them
across all connected browsers.
"""

from __future__ import annotations

import json
import os
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

# --- Configuration -----------------------------------------------------------

# Default to loopback; set HOST=0.0.0.0 to expose on your LAN.
HOST = os.getenv("HOST", "127.0.0.1")
PORT = int(os.getenv("PORT", "8000"))

FRONTEND_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "frontend")

OPENSKY_STATES_URL = "https://opensky-network.org/api/states/all"
# Public OAuth2 endpoint URL (not a credential); S105 false positive on "TOKEN".
OPENSKY_TOKEN_URL = (
    "https://auth.opensky-network.org/auth/realms/opensky-network/protocol/openid-connect/token"  # noqa: S105
)

OPENSKY_CLIENT_ID = os.getenv("OPENSKY_CLIENT_ID", "").strip()
OPENSKY_CLIENT_SECRET = os.getenv("OPENSKY_CLIENT_SECRET", "").strip()

# How long (seconds) a proxied OpenSky response is reused before refetching.
# OpenSky updates roughly every 5-10s and rate limits aggressively, so a
# short shared cache keeps us well within limits while staying live.
CACHE_TTL_SECONDS = float(os.getenv("CACHE_TTL_SECONDS", "8"))

REQUEST_TIMEOUT = 20

# Column order of the OpenSky "states" vectors. See:
# https://openskynetwork.github.io/opensky-api/rest.html#response
STATE_FIELDS = [
    "icao24",
    "callsign",
    "origin_country",
    "time_position",
    "last_contact",
    "longitude",
    "latitude",
    "baro_altitude",
    "on_ground",
    "velocity",
    "true_track",
    "vertical_rate",
    "sensors",
    "geo_altitude",
    "squawk",
    "spi",
    "position_source",
    "category",
]

# Fields we forward to the browser (dropping sensor arrays we don't use).
FORWARD_FIELDS = [
    "icao24",
    "callsign",
    "origin_country",
    "longitude",
    "latitude",
    "baro_altitude",
    "geo_altitude",
    "on_ground",
    "velocity",
    "true_track",
    "vertical_rate",
    "squawk",
    "last_contact",
]


# --- OAuth2 token handling ---------------------------------------------------


class TokenManager:
    """Fetches and caches an OpenSky OAuth2 access token when credentials
    are configured. Falls back to anonymous access otherwise."""

    def __init__(self, client_id: str, client_secret: str) -> None:
        self.client_id = client_id
        self.client_secret = client_secret
        self._lock = threading.Lock()
        self._token = ""
        self._expires_at = 0.0

    @property
    def enabled(self) -> bool:
        return bool(self.client_id and self.client_secret)

    def get_token(self) -> str | None:
        if not self.enabled:
            return None
        with self._lock:
            if self._token and time.time() < self._expires_at - 30:
                return self._token
            self._refresh()
            return self._token or None

    def _refresh(self) -> None:
        data = urllib.parse.urlencode(
            {
                "grant_type": "client_credentials",
                "client_id": self.client_id,
                "client_secret": self.client_secret,
            }
        ).encode()
        req = urllib.request.Request(  # noqa: S310 - fixed HTTPS OAuth2 endpoint
            OPENSKY_TOKEN_URL,
            data=data,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
            method="POST",
        )
        try:
            with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT) as resp:  # noqa: S310
                payload = json.loads(resp.read().decode())
            self._token = payload.get("access_token", "")
            self._expires_at = time.time() + float(payload.get("expires_in", 300))
            print("[auth] obtained OpenSky access token")
        except Exception as exc:  # noqa: BLE001 - log and fall back to anonymous
            self._token = ""
            self._expires_at = 0.0
            print(f"[auth] failed to obtain token, using anonymous access: {exc}")


TOKENS = TokenManager(OPENSKY_CLIENT_ID, OPENSKY_CLIENT_SECRET)


# --- Live-states cache -------------------------------------------------------


class StatesCache:
    """A tiny per-bounding-box cache so many browsers share one upstream
    fetch and we stay under OpenSky's rate limits."""

    def __init__(self, ttl: float) -> None:
        self.ttl = ttl
        self._lock = threading.Lock()
        self._entries: dict[str, tuple[float, dict]] = {}

    def get(self, key: str) -> dict | None:
        with self._lock:
            entry = self._entries.get(key)
            if entry and time.time() - entry[0] < self.ttl:
                return entry[1]
        return None

    def set(self, key: str, value: dict) -> None:
        with self._lock:
            self._entries[key] = (time.time(), value)
            # Bound memory: keep only the most recent handful of viewports.
            if len(self._entries) > 32:
                oldest = min(self._entries, key=lambda k: self._entries[k][0])
                del self._entries[oldest]


CACHE = StatesCache(CACHE_TTL_SECONDS)


def _bbox_from_query(query: dict[str, list[str]]) -> dict[str, float] | None:
    """Parse an optional lat/lon bounding box from the query string."""
    keys = ("lamin", "lomin", "lamax", "lomax")
    if not all(k in query for k in keys):
        return None
    try:
        return {k: float(query[k][0]) for k in keys}
    except (ValueError, IndexError):
        return None


def _transform_states(raw: dict) -> dict:
    """Convert OpenSky's positional arrays into named-field objects and
    drop entries without a usable position."""
    states = raw.get("states") or []
    fwd_index = {name: STATE_FIELDS.index(name) for name in FORWARD_FIELDS}
    aircraft = []
    for vec in states:
        if len(vec) < len(STATE_FIELDS):
            # Older/authenticated responses may omit the trailing "category".
            vec = list(vec) + [None] * (len(STATE_FIELDS) - len(vec))
        lon = vec[STATE_FIELDS.index("longitude")]
        lat = vec[STATE_FIELDS.index("latitude")]
        if lon is None or lat is None:
            continue
        obj = {name: vec[idx] for name, idx in fwd_index.items()}
        if isinstance(obj.get("callsign"), str):
            obj["callsign"] = obj["callsign"].strip()
        aircraft.append(obj)
    return {"time": raw.get("time"), "count": len(aircraft), "aircraft": aircraft}


def fetch_states(bbox: dict[str, float] | None) -> dict:
    """Fetch (and cache) live aircraft states from OpenSky."""
    cache_key = (
        "global"
        if bbox is None
        else "{lamin:.2f},{lomin:.2f},{lamax:.2f},{lomax:.2f}".format(**bbox)
    )
    cached = CACHE.get(cache_key)
    if cached is not None:
        return cached

    url = OPENSKY_STATES_URL
    if bbox is not None:
        url += "?" + urllib.parse.urlencode(bbox)

    headers = {"User-Agent": "worldwide-flight-tracker/1.0"}
    token = TOKENS.get_token()
    if token:
        headers["Authorization"] = f"Bearer {token}"

    if not url.startswith("https://"):  # defensive: only ever the OpenSky endpoint
        raise ValueError("refusing to fetch a non-HTTPS URL")
    req = urllib.request.Request(url, headers=headers)  # noqa: S310 - guarded HTTPS above
    with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT) as resp:  # noqa: S310
        raw = json.loads(resp.read().decode())

    result = _transform_states(raw)
    CACHE.set(cache_key, result)
    return result


# --- HTTP request handling ---------------------------------------------------


class Handler(BaseHTTPRequestHandler):
    server_version = "FlightTracker/1.0"

    def log_message(self, fmt: str, *args) -> None:  # quieter logging
        if os.getenv("VERBOSE"):
            super().log_message(fmt, *args)

    def _send_json(self, obj: dict, status: int = 200) -> None:
        body = json.dumps(obj).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:  # noqa: N802 - stdlib naming
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path

        if path == "/api/states":
            self._handle_states(parsed)
            return
        if path == "/api/health":
            self._send_json(
                {"status": "ok", "authenticated": TOKENS.enabled, "cache_ttl": CACHE.ttl}
            )
            return

        self._serve_static(path)

    def _handle_states(self, parsed: urllib.parse.ParseResult) -> None:
        query = urllib.parse.parse_qs(parsed.query)
        bbox = _bbox_from_query(query)
        try:
            data = fetch_states(bbox)
            self._send_json(data)
        except urllib.error.HTTPError as exc:
            status = exc.code
            hint = ""
            if status == 429:
                hint = " (OpenSky rate limit reached — the app will retry shortly)"
            self._send_json(
                {"error": f"OpenSky returned HTTP {status}{hint}", "aircraft": []},
                status=502,
            )
        except Exception as exc:  # noqa: BLE001
            self._send_json(
                {"error": f"Failed to fetch flight data: {exc}", "aircraft": []},
                status=502,
            )

    def _serve_static(self, path: str) -> None:
        if path in ("", "/"):
            path = "/index.html"
        # Prevent path traversal outside the frontend directory.
        safe = os.path.normpath(path).lstrip("/\\")
        full = os.path.join(FRONTEND_DIR, safe)
        if not os.path.abspath(full).startswith(os.path.abspath(FRONTEND_DIR)):
            self.send_error(403, "Forbidden")
            return
        if not os.path.isfile(full):
            self.send_error(404, "Not Found")
            return

        ctype = _content_type(full)
        try:
            with open(full, "rb") as fh:
                body = fh.read()
        except OSError:
            self.send_error(404, "Not Found")
            return
        self.send_response(200)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def _content_type(path: str) -> str:
    ext = os.path.splitext(path)[1].lower()
    return {
        ".html": "text/html; charset=utf-8",
        ".css": "text/css; charset=utf-8",
        ".js": "application/javascript; charset=utf-8",
        ".json": "application/json",
        ".svg": "image/svg+xml",
        ".png": "image/png",
        ".ico": "image/x-icon",
    }.get(ext, "application/octet-stream")


def main() -> None:
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    auth = "authenticated" if TOKENS.enabled else "anonymous"
    print("Worldwide Flight Tracker")
    print(f"  serving frontend from {FRONTEND_DIR}")
    print(f"  OpenSky access: {auth} (cache TTL {CACHE.ttl:.0f}s)")
    print(f"  open http://localhost:{PORT} in your browser")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nshutting down")
        server.shutdown()


if __name__ == "__main__":
    main()
