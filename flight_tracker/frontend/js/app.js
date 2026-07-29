/* Worldwide Flight Tracker — main application. */

(function () {
  "use strict";

  // ---- State --------------------------------------------------------------
  const state = {
    map: null,
    aircraftLayer: null,
    trailLayer: null,
    markers: new Map(), // icao24 -> { marker, data }
    trails: new Map(), // icao24 -> { line, points: [[lat,lon],...] }
    selected: null, // icao24 of selected aircraft
    following: false,
    lastData: [], // most recent aircraft array (for search)
    timer: null,
    inFlight: false,
    filters: {
      minAltitude: 0, // metres
      showGround: true,
      autoRefresh: true,
      showTrails: true,
    },
  };

  // ---- Plane icon ---------------------------------------------------------
  function planeSvg(color) {
    // A top-down plane silhouette pointing "up" (north = 0°).
    return (
      `<svg width="26" height="26" viewBox="0 0 24 24" fill="${color}" ` +
      `xmlns="http://www.w3.org/2000/svg">` +
      `<path d="M12 2 L13.2 3.2 13.2 9 21 14 21 15.8 13.2 13.4 13.2 19 ` +
      `15.4 20.6 15.4 22 12 21 8.6 22 8.6 20.6 10.8 19 10.8 13.4 3 15.8 ` +
      `3 14 10.8 9 10.8 3.2 Z"/></svg>`
    );
  }

  function makeIcon(ac, selected) {
    const color = ac.on_ground ? "#9aa4b8" : altitudeColor(ac.baro_altitude);
    const heading = ac.true_track || 0;
    const cls = "plane-icon" + (selected ? " selected" : "");
    return L.divIcon({
      className: "aircraft-marker",
      html: `<div class="${cls}" style="transform: rotate(${heading}deg)">${planeSvg(
        color
      )}</div>`,
      iconSize: [26, 26],
      iconAnchor: [13, 13],
    });
  }

  // ---- Map setup ----------------------------------------------------------
  function initMap() {
    const map = L.map("map", {
      center: CONFIG.initialCenter,
      zoom: CONFIG.initialZoom,
      worldCopyJump: true,
      minZoom: 2,
      maxZoom: 12,
      zoomControl: true,
      attributionControl: true,
    });

    L.tileLayer(
      "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png",
      {
        attribution:
          '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> ' +
          '&copy; <a href="https://carto.com/attributions">CARTO</a> · ' +
          'Flight data <a href="https://opensky-network.org">OpenSky Network</a>',
        subdomains: "abcd",
        maxZoom: 19,
      }
    ).addTo(map);

    state.trailLayer = L.layerGroup().addTo(map);
    state.aircraftLayer = L.layerGroup().addTo(map);
    state.map = map;

    map.on("moveend", () => {
      // When the user pans/zooms, refresh the viewport promptly.
      scheduleImmediate();
    });
    map.on("click", () => deselect());
  }

  // ---- Data refresh -------------------------------------------------------
  async function refresh() {
    if (state.inFlight) return;
    state.inFlight = true;
    try {
      const zoom = state.map.getZoom();
      const bounds = zoom >= CONFIG.viewportFetchMinZoom ? state.map.getBounds() : null;
      const data = await API.fetchStates(bounds);
      state.lastData = data.aircraft || [];
      render(state.lastData);
      setConnection("ok");
      setUpdated(data.time);
    } catch (err) {
      setConnection("err");
      showToast(`⚠ ${err.message}`);
    } finally {
      state.inFlight = false;
    }
  }

  function passesFilters(ac) {
    if (!state.filters.showGround && ac.on_ground) return false;
    if (!ac.on_ground && (ac.baro_altitude || 0) < state.filters.minAltitude) {
      return false;
    }
    return true;
  }

  function render(aircraft) {
    const seen = new Set();
    let visible = 0;

    for (const ac of aircraft) {
      if (ac.latitude == null || ac.longitude == null) continue;
      if (!passesFilters(ac) && ac.icao24 !== state.selected) continue;
      seen.add(ac.icao24);
      visible++;
      upsertMarker(ac);
      if (state.filters.showTrails) updateTrail(ac);
    }

    // Remove markers no longer present (unless selected/followed).
    for (const [id, entry] of state.markers) {
      if (!seen.has(id) && id !== state.selected) {
        state.aircraftLayer.removeLayer(entry.marker);
        state.markers.delete(id);
        removeTrail(id);
      }
    }

    document.getElementById("stat-visible").textContent = visible.toLocaleString();

    // Keep the detail panel live for the selected aircraft.
    if (state.selected) {
      const cur = aircraft.find((a) => a.icao24 === state.selected);
      if (cur) {
        fillDetail(cur);
        if (state.following && cur.latitude != null) {
          state.map.panTo([cur.latitude, cur.longitude], { animate: true });
        }
      }
    }
  }

  function upsertMarker(ac) {
    const pos = [ac.latitude, ac.longitude];
    const existing = state.markers.get(ac.icao24);
    const isSel = ac.icao24 === state.selected;

    if (existing) {
      existing.marker.setLatLng(pos);
      existing.data = ac;
      // Update rotation + colour in place for a smooth feel.
      const el = existing.marker.getElement();
      if (el) {
        const inner = el.querySelector(".plane-icon");
        if (inner) {
          inner.style.transform = `rotate(${ac.true_track || 0}deg)`;
          const color = ac.on_ground ? "#9aa4b8" : altitudeColor(ac.baro_altitude);
          const path = inner.querySelector("path");
          if (path) path.setAttribute("fill", color);
          inner.classList.toggle("selected", isSel);
        }
      }
    } else {
      const marker = L.marker(pos, {
        icon: makeIcon(ac, isSel),
        riseOnHover: true,
        keyboard: false,
      });
      marker.on("click", (e) => {
        L.DomEvent.stopPropagation(e);
        select(ac.icao24);
      });
      marker.addTo(state.aircraftLayer);
      state.markers.set(ac.icao24, { marker, data: ac });
    }
  }

  // ---- Trails -------------------------------------------------------------
  function updateTrail(ac) {
    if (ac.on_ground) return;
    let trail = state.trails.get(ac.icao24);
    const point = [ac.latitude, ac.longitude];
    if (!trail) {
      trail = { points: [], line: null };
      state.trails.set(ac.icao24, trail);
    }
    const last = trail.points[trail.points.length - 1];
    if (!last || last[0] !== point[0] || last[1] !== point[1]) {
      trail.points.push(point);
      if (trail.points.length > CONFIG.maxTrailPoints) trail.points.shift();
    }
    if (trail.points.length >= 2) {
      const color = altitudeColor(ac.baro_altitude);
      if (!trail.line) {
        trail.line = L.polyline(trail.points, {
          color,
          weight: ac.icao24 === state.selected ? 3 : 1.5,
          opacity: ac.icao24 === state.selected ? 0.9 : 0.35,
        }).addTo(state.trailLayer);
      } else {
        trail.line.setLatLngs(trail.points);
        trail.line.setStyle({
          color,
          weight: ac.icao24 === state.selected ? 3 : 1.5,
          opacity: ac.icao24 === state.selected ? 0.9 : 0.35,
        });
      }
    }
  }

  function removeTrail(id) {
    const trail = state.trails.get(id);
    if (trail && trail.line) state.trailLayer.removeLayer(trail.line);
    state.trails.delete(id);
  }

  function clearAllTrails() {
    for (const id of Array.from(state.trails.keys())) removeTrail(id);
  }

  // ---- Selection & detail panel ------------------------------------------
  function select(icao24) {
    state.selected = icao24;
    const entry = state.markers.get(icao24);
    if (entry) {
      const el = entry.marker.getElement();
      const inner = el && el.querySelector(".plane-icon");
      if (inner) inner.classList.add("selected");
      fillDetail(entry.data);
    }
    const ac = state.lastData.find((a) => a.icao24 === icao24);
    if (ac) fillDetail(ac);
    document.getElementById("detail").classList.remove("hidden");
    hideSearchResults();
  }

  function deselect() {
    if (!state.selected) return;
    const entry = state.markers.get(state.selected);
    if (entry) {
      const el = entry.marker.getElement();
      const inner = el && el.querySelector(".plane-icon");
      if (inner) inner.classList.remove("selected");
    }
    state.selected = null;
    state.following = false;
    document.getElementById("d-follow").classList.remove("active");
    document.getElementById("d-follow").textContent = "Follow this aircraft";
    document.getElementById("detail").classList.add("hidden");
  }

  function fillDetail(ac) {
    const g = (id) => document.getElementById(id);
    g("d-callsign").textContent = ac.callsign || "(no callsign)";
    g("d-country").textContent = ac.origin_country || "Unknown origin";
    g("d-altitude").textContent = fmtAltitude(ac.baro_altitude);
    g("d-speed").textContent = fmtSpeed(ac.velocity);
    g("d-heading").textContent = fmtHeading(ac.true_track);
    g("d-vrate").textContent = fmtVerticalRate(ac.vertical_rate);
    g("d-position").textContent = fmtCoord(ac.latitude, ac.longitude);
    g("d-icao").textContent = (ac.icao24 || "—").toUpperCase();
    g("d-squawk").textContent = ac.squawk || "—";
    g("d-status").textContent = ac.on_ground ? "On ground" : "Airborne";
    const link = g("d-external");
    link.href = `https://opensky-network.org/aircraft-profile?icao24=${ac.icao24}`;
  }

  // ---- Search -------------------------------------------------------------
  function runSearch(query) {
    const q = query.trim().toLowerCase();
    const box = document.getElementById("search-results");
    if (!q) return hideSearchResults();

    const matches = state.lastData
      .filter((ac) => {
        return (
          (ac.callsign && ac.callsign.toLowerCase().includes(q)) ||
          (ac.icao24 && ac.icao24.toLowerCase().includes(q)) ||
          (ac.origin_country && ac.origin_country.toLowerCase().includes(q))
        );
      })
      .slice(0, 40);

    if (matches.length === 0) {
      box.innerHTML = '<div class="search-empty">No matching aircraft in view</div>';
      box.classList.remove("hidden");
      return;
    }

    box.innerHTML = matches
      .map(
        (ac) =>
          `<div class="search-item" data-icao="${ac.icao24}">` +
          `<span class="si-call">${ac.callsign || ac.icao24.toUpperCase()}</span>` +
          `<span class="si-meta">${ac.origin_country || ""} · ${fmtAltitude(
            ac.baro_altitude
          )}</span></div>`
      )
      .join("");
    box.classList.remove("hidden");

    box.querySelectorAll(".search-item").forEach((item) => {
      item.addEventListener("click", () => {
        const icao = item.getAttribute("data-icao");
        const ac = state.lastData.find((a) => a.icao24 === icao);
        if (ac && ac.latitude != null) {
          state.map.setView([ac.latitude, ac.longitude], Math.max(state.map.getZoom(), 7));
          select(icao);
        }
        document.getElementById("search-input").value = "";
        hideSearchResults();
      });
    });
  }

  function hideSearchResults() {
    document.getElementById("search-results").classList.add("hidden");
  }

  // ---- Small UI helpers ---------------------------------------------------
  function setConnection(status) {
    const dot = document.getElementById("conn-dot");
    dot.className = "conn-dot " + (status === "ok" ? "ok" : "err");
    dot.title = status === "ok" ? "Live" : "Connection problem";
  }

  function setUpdated(epochSeconds) {
    const el = document.getElementById("stat-updated");
    const d = epochSeconds ? new Date(epochSeconds * 1000) : new Date();
    el.textContent = d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
  }

  let toastTimer = null;
  function showToast(msg) {
    const t = document.getElementById("toast");
    t.textContent = msg;
    t.classList.remove("hidden");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => t.classList.add("hidden"), 5000);
  }

  // ---- Polling scheduling -------------------------------------------------
  function startPolling() {
    stopPolling();
    if (state.filters.autoRefresh) {
      state.timer = setInterval(refresh, CONFIG.refreshInterval);
    }
  }
  function stopPolling() {
    if (state.timer) clearInterval(state.timer);
    state.timer = null;
  }
  let immediateTimer = null;
  function scheduleImmediate() {
    clearTimeout(immediateTimer);
    immediateTimer = setTimeout(refresh, 400);
  }

  // ---- Wiring UI ----------------------------------------------------------
  function wireUI() {
    document.getElementById("filters-toggle").addEventListener("click", () => {
      document.getElementById("filters").classList.toggle("collapsed");
    });

    const altMin = document.getElementById("alt-min");
    altMin.addEventListener("input", () => {
      const ft = Number(altMin.value);
      state.filters.minAltitude = ft / CONFIG.M_TO_FT;
      document.getElementById("alt-min-label").textContent = `${ft.toLocaleString()} ft`;
      render(state.lastData);
    });

    document.getElementById("show-ground").addEventListener("change", (e) => {
      state.filters.showGround = e.target.checked;
      render(state.lastData);
    });

    document.getElementById("show-trails").addEventListener("change", (e) => {
      state.filters.showTrails = e.target.checked;
      if (!e.target.checked) clearAllTrails();
    });

    document.getElementById("auto-refresh").addEventListener("change", (e) => {
      state.filters.autoRefresh = e.target.checked;
      startPolling();
    });

    document.getElementById("refresh-now").addEventListener("click", refresh);
    document.getElementById("detail-close").addEventListener("click", deselect);

    document.getElementById("d-follow").addEventListener("click", (e) => {
      state.following = !state.following;
      e.target.classList.toggle("active", state.following);
      e.target.textContent = state.following ? "Following ✓" : "Follow this aircraft";
    });

    const searchInput = document.getElementById("search-input");
    searchInput.addEventListener("input", () => runSearch(searchInput.value));
    searchInput.addEventListener("focus", () => {
      if (searchInput.value) runSearch(searchInput.value);
    });
    document.addEventListener("click", (e) => {
      if (!e.target.closest(".search")) hideSearchResults();
    });

    document.getElementById("refresh-secs").textContent = String(
      CONFIG.refreshInterval / 1000
    );
  }

  // ---- Boot ---------------------------------------------------------------
  function boot() {
    initMap();
    wireUI();
    refresh();
    startPolling();
  }

  document.addEventListener("DOMContentLoaded", boot);
})();
