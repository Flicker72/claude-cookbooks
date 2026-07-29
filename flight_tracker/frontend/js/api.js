/* Thin wrapper around the backend flight-states endpoint. */

const API = {
  /**
   * Fetch live aircraft states, optionally constrained to a map bounding box.
   * @param {L.LatLngBounds|null} bounds Leaflet bounds, or null for global.
   * @returns {Promise<{time:number, count:number, aircraft:Array}>}
   */
  async fetchStates(bounds) {
    let url = "/api/states";
    if (bounds) {
      const sw = bounds.getSouthWest();
      const ne = bounds.getNorthEast();
      const params = new URLSearchParams({
        lamin: clampLat(sw.lat).toFixed(4),
        lomin: clampLon(sw.lng).toFixed(4),
        lamax: clampLat(ne.lat).toFixed(4),
        lomax: clampLon(ne.lng).toFixed(4),
      });
      url += `?${params.toString()}`;
    }

    const resp = await fetch(url, { headers: { Accept: "application/json" } });
    const data = await resp.json();
    if (!resp.ok || data.error) {
      throw new Error(data.error || `Request failed (${resp.status})`);
    }
    return data;
  },
};

function clampLat(v) {
  return Math.max(-90, Math.min(90, v));
}
function clampLon(v) {
  return Math.max(-180, Math.min(180, v));
}
