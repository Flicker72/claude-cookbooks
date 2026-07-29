/* ICAO airline (operator) codes -> { name, country }.
 *
 * A flight's callsign begins with the operator's 3-letter ICAO code
 * (e.g. "BAW123" -> British Airways). This is a curated set of major
 * operators worldwide; unknown prefixes simply fall back to the raw
 * callsign. Not exhaustive — extend as needed. */

const AIRLINES = {
  // --- United Kingdom & Ireland ---
  BAW: { name: "British Airways", country: "United Kingdom" },
  SHT: { name: "British Airways Shuttle", country: "United Kingdom" },
  EZY: { name: "easyJet", country: "United Kingdom" },
  EXS: { name: "Jet2", country: "United Kingdom" },
  TOM: { name: "TUI Airways", country: "United Kingdom" },
  VIR: { name: "Virgin Atlantic", country: "United Kingdom" },
  RYR: { name: "Ryanair", country: "Ireland" },
  EIN: { name: "Aer Lingus", country: "Ireland" },

  // --- Western & Central Europe ---
  DLH: { name: "Lufthansa", country: "Germany" },
  CLH: { name: "Lufthansa CityLine", country: "Germany" },
  EWG: { name: "Eurowings", country: "Germany" },
  CFG: { name: "Condor", country: "Germany" },
  GEC: { name: "Lufthansa Cargo", country: "Germany" },
  AFR: { name: "Air France", country: "France" },
  TVF: { name: "Transavia France", country: "France" },
  KLM: { name: "KLM", country: "Netherlands" },
  TRA: { name: "Transavia", country: "Netherlands" },
  SWR: { name: "SWISS", country: "Switzerland" },
  AUA: { name: "Austrian Airlines", country: "Austria" },
  BEL: { name: "Brussels Airlines", country: "Belgium" },
  LGL: { name: "Luxair", country: "Luxembourg" },
  CLX: { name: "Cargolux", country: "Luxembourg" },

  // --- Nordics ---
  SAS: { name: "Scandinavian Airlines", country: "Sweden" },
  NAX: { name: "Norwegian Air Shuttle", country: "Norway" },
  NOZ: { name: "Norwegian Air", country: "Norway" },
  FIN: { name: "Finnair", country: "Finland" },
  ICE: { name: "Icelandair", country: "Iceland" },

  // --- Southern Europe ---
  IBE: { name: "Iberia", country: "Spain" },
  VLG: { name: "Vueling", country: "Spain" },
  AEA: { name: "Air Europa", country: "Spain" },
  TAP: { name: "TAP Air Portugal", country: "Portugal" },
  ITY: { name: "ITA Airways", country: "Italy" },
  AEE: { name: "Aegean Airlines", country: "Greece" },

  // --- Eastern Europe & Turkey ---
  THY: { name: "Turkish Airlines", country: "Turkey" },
  PGT: { name: "Pegasus Airlines", country: "Turkey" },
  LOT: { name: "LOT Polish Airlines", country: "Poland" },
  CSA: { name: "Czech Airlines", country: "Czechia" },
  WZZ: { name: "Wizz Air", country: "Hungary" },
  AFL: { name: "Aeroflot", country: "Russia" },
  SBI: { name: "S7 Airlines", country: "Russia" },
  SVR: { name: "Ural Airlines", country: "Russia" },

  // --- North America ---
  UAL: { name: "United Airlines", country: "United States" },
  AAL: { name: "American Airlines", country: "United States" },
  DAL: { name: "Delta Air Lines", country: "United States" },
  SWA: { name: "Southwest Airlines", country: "United States" },
  JBU: { name: "JetBlue", country: "United States" },
  ASA: { name: "Alaska Airlines", country: "United States" },
  FFT: { name: "Frontier Airlines", country: "United States" },
  NKS: { name: "Spirit Airlines", country: "United States" },
  HAL: { name: "Hawaiian Airlines", country: "United States" },
  UPS: { name: "UPS Airlines", country: "United States" },
  FDX: { name: "FedEx Express", country: "United States" },
  ACA: { name: "Air Canada", country: "Canada" },
  WJA: { name: "WestJet", country: "Canada" },
  JZA: { name: "Air Canada Jazz", country: "Canada" },
  AMX: { name: "Aeroméxico", country: "Mexico" },
  VOI: { name: "Volaris", country: "Mexico" },

  // --- Central & South America ---
  LAN: { name: "LATAM Airlines", country: "Chile" },
  TAM: { name: "LATAM Brasil", country: "Brazil" },
  GLO: { name: "Gol Linhas Aéreas", country: "Brazil" },
  AZU: { name: "Azul", country: "Brazil" },
  ARG: { name: "Aerolíneas Argentinas", country: "Argentina" },
  AVA: { name: "Avianca", country: "Colombia" },
  CMP: { name: "Copa Airlines", country: "Panama" },

  // --- Middle East ---
  UAE: { name: "Emirates", country: "United Arab Emirates" },
  ETD: { name: "Etihad Airways", country: "United Arab Emirates" },
  FDB: { name: "flydubai", country: "United Arab Emirates" },
  ABY: { name: "Air Arabia", country: "United Arab Emirates" },
  QTR: { name: "Qatar Airways", country: "Qatar" },
  SVA: { name: "Saudia", country: "Saudi Arabia" },
  GFA: { name: "Gulf Air", country: "Bahrain" },
  KAC: { name: "Kuwait Airways", country: "Kuwait" },
  MEA: { name: "Middle East Airlines", country: "Lebanon" },
  ELY: { name: "El Al", country: "Israel" },
  RJA: { name: "Royal Jordanian", country: "Jordan" },

  // --- East Asia ---
  JAL: { name: "Japan Airlines", country: "Japan" },
  ANA: { name: "All Nippon Airways", country: "Japan" },
  KAL: { name: "Korean Air", country: "South Korea" },
  AAR: { name: "Asiana Airlines", country: "South Korea" },
  JJA: { name: "Jeju Air", country: "South Korea" },
  CCA: { name: "Air China", country: "China" },
  CES: { name: "China Eastern", country: "China" },
  CSN: { name: "China Southern", country: "China" },
  CHH: { name: "Hainan Airlines", country: "China" },
  CXA: { name: "Xiamen Airlines", country: "China" },
  CSC: { name: "Sichuan Airlines", country: "China" },
  CAL: { name: "China Airlines", country: "Taiwan" },
  EVA: { name: "EVA Air", country: "Taiwan" },
  CPA: { name: "Cathay Pacific", country: "Hong Kong" },

  // --- South & Southeast Asia ---
  SIA: { name: "Singapore Airlines", country: "Singapore" },
  TGW: { name: "Scoot", country: "Singapore" },
  THA: { name: "Thai Airways", country: "Thailand" },
  MAS: { name: "Malaysia Airlines", country: "Malaysia" },
  AXM: { name: "AirAsia", country: "Malaysia" },
  GIA: { name: "Garuda Indonesia", country: "Indonesia" },
  LNI: { name: "Lion Air", country: "Indonesia" },
  PAL: { name: "Philippine Airlines", country: "Philippines" },
  CEB: { name: "Cebu Pacific", country: "Philippines" },
  HVN: { name: "Vietnam Airlines", country: "Vietnam" },
  VJC: { name: "VietJet Air", country: "Vietnam" },
  AIC: { name: "Air India", country: "India" },
  IGO: { name: "IndiGo", country: "India" },
  SEJ: { name: "SpiceJet", country: "India" },
  VTI: { name: "Vistara", country: "India" },

  // --- Oceania ---
  QFA: { name: "Qantas", country: "Australia" },
  JST: { name: "Jetstar", country: "Australia" },
  VOZ: { name: "Virgin Australia", country: "Australia" },
  ANZ: { name: "Air New Zealand", country: "New Zealand" },

  // --- Africa ---
  ETH: { name: "Ethiopian Airlines", country: "Ethiopia" },
  SAA: { name: "South African Airways", country: "South Africa" },
  RAM: { name: "Royal Air Maroc", country: "Morocco" },
  MSR: { name: "EgyptAir", country: "Egypt" },
  KQA: { name: "Kenya Airways", country: "Kenya" },
  DAH: { name: "Air Algérie", country: "Algeria" },

  // --- Cargo & other ---
  BCS: { name: "DHL Air", country: "Belgium" },
  DHL: { name: "DHL", country: "International" },
};

/**
 * Resolve an airline from a callsign's leading 3-letter ICAO prefix.
 * @param {string} callsign e.g. "BAW123"
 * @returns {{name:string, country:string, code:string}|null}
 */
function lookupAirline(callsign) {
  if (!callsign || callsign.length < 3) return null;
  const prefix = callsign.slice(0, 3).toUpperCase();
  // Airline callsigns are three letters followed by a flight number
  // (e.g. "BAW123"). Registration-style callsigns such as "N123AB" or
  // "G-ABCD" are individual aircraft, not operators, so skip them.
  if (!/^[A-Z]{3}[0-9]/.test(callsign.toUpperCase())) return null;
  const info = AIRLINES[prefix];
  return info ? { ...info, code: prefix } : null;
}
