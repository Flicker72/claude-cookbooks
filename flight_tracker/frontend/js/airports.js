/* A curated set of major world airports, rendered as an optional map
 * layer. Fields: icao, iata, name, city, country, lat, lon.
 * Coordinates are approximate airport-reference points (good enough for
 * a display marker). Not exhaustive — extend as needed. */

const AIRPORTS = [
  // --- Europe ---
  { icao: "EGLL", iata: "LHR", name: "Heathrow", city: "London", country: "United Kingdom", lat: 51.4706, lon: -0.4619 },
  { icao: "EGKK", iata: "LGW", name: "Gatwick", city: "London", country: "United Kingdom", lat: 51.1481, lon: -0.1903 },
  { icao: "EGCC", iata: "MAN", name: "Manchester", city: "Manchester", country: "United Kingdom", lat: 53.3537, lon: -2.275 },
  { icao: "EIDW", iata: "DUB", name: "Dublin", city: "Dublin", country: "Ireland", lat: 53.4213, lon: -6.2701 },
  { icao: "LFPG", iata: "CDG", name: "Charles de Gaulle", city: "Paris", country: "France", lat: 49.0097, lon: 2.5479 },
  { icao: "LFPO", iata: "ORY", name: "Orly", city: "Paris", country: "France", lat: 48.7233, lon: 2.3794 },
  { icao: "EHAM", iata: "AMS", name: "Schiphol", city: "Amsterdam", country: "Netherlands", lat: 52.3105, lon: 4.7683 },
  { icao: "EDDF", iata: "FRA", name: "Frankfurt", city: "Frankfurt", country: "Germany", lat: 50.0379, lon: 8.5622 },
  { icao: "EDDM", iata: "MUC", name: "Munich", city: "Munich", country: "Germany", lat: 48.3538, lon: 11.7861 },
  { icao: "EDDB", iata: "BER", name: "Brandenburg", city: "Berlin", country: "Germany", lat: 52.3667, lon: 13.5033 },
  { icao: "LSZH", iata: "ZRH", name: "Zürich", city: "Zürich", country: "Switzerland", lat: 47.4647, lon: 8.5492 },
  { icao: "LOWW", iata: "VIE", name: "Vienna", city: "Vienna", country: "Austria", lat: 48.1103, lon: 16.5697 },
  { icao: "EBBR", iata: "BRU", name: "Brussels", city: "Brussels", country: "Belgium", lat: 50.9014, lon: 4.4844 },
  { icao: "LEMD", iata: "MAD", name: "Barajas", city: "Madrid", country: "Spain", lat: 40.4936, lon: -3.5668 },
  { icao: "LEBL", iata: "BCN", name: "El Prat", city: "Barcelona", country: "Spain", lat: 41.2971, lon: 2.0785 },
  { icao: "LPPT", iata: "LIS", name: "Humberto Delgado", city: "Lisbon", country: "Portugal", lat: 38.7742, lon: -9.1342 },
  { icao: "LIRF", iata: "FCO", name: "Fiumicino", city: "Rome", country: "Italy", lat: 41.8003, lon: 12.2389 },
  { icao: "LIMC", iata: "MXP", name: "Malpensa", city: "Milan", country: "Italy", lat: 45.6306, lon: 8.7281 },
  { icao: "LGAV", iata: "ATH", name: "Eleftherios Venizelos", city: "Athens", country: "Greece", lat: 37.9364, lon: 23.9445 },
  { icao: "LTFM", iata: "IST", name: "Istanbul", city: "Istanbul", country: "Turkey", lat: 41.2753, lon: 28.7519 },
  { icao: "EKCH", iata: "CPH", name: "Kastrup", city: "Copenhagen", country: "Denmark", lat: 55.6181, lon: 12.656 },
  { icao: "ESSA", iata: "ARN", name: "Arlanda", city: "Stockholm", country: "Sweden", lat: 59.6519, lon: 17.9186 },
  { icao: "ENGM", iata: "OSL", name: "Gardermoen", city: "Oslo", country: "Norway", lat: 60.1939, lon: 11.1004 },
  { icao: "EFHK", iata: "HEL", name: "Helsinki-Vantaa", city: "Helsinki", country: "Finland", lat: 60.3172, lon: 24.9633 },
  { icao: "UUEE", iata: "SVO", name: "Sheremetyevo", city: "Moscow", country: "Russia", lat: 55.9726, lon: 37.4146 },
  { icao: "EPWA", iata: "WAW", name: "Chopin", city: "Warsaw", country: "Poland", lat: 52.1657, lon: 20.9671 },

  // --- North America ---
  { icao: "KATL", iata: "ATL", name: "Hartsfield-Jackson", city: "Atlanta", country: "United States", lat: 33.6367, lon: -84.4281 },
  { icao: "KLAX", iata: "LAX", name: "Los Angeles Intl", city: "Los Angeles", country: "United States", lat: 33.9425, lon: -118.4081 },
  { icao: "KORD", iata: "ORD", name: "O'Hare", city: "Chicago", country: "United States", lat: 41.9786, lon: -87.9048 },
  { icao: "KDFW", iata: "DFW", name: "Dallas/Fort Worth", city: "Dallas", country: "United States", lat: 32.8968, lon: -97.038 },
  { icao: "KDEN", iata: "DEN", name: "Denver Intl", city: "Denver", country: "United States", lat: 39.8617, lon: -104.673 },
  { icao: "KJFK", iata: "JFK", name: "John F. Kennedy", city: "New York", country: "United States", lat: 40.6398, lon: -73.7789 },
  { icao: "KEWR", iata: "EWR", name: "Newark Liberty", city: "Newark", country: "United States", lat: 40.6925, lon: -74.1687 },
  { icao: "KSFO", iata: "SFO", name: "San Francisco Intl", city: "San Francisco", country: "United States", lat: 37.6189, lon: -122.375 },
  { icao: "KSEA", iata: "SEA", name: "Seattle-Tacoma", city: "Seattle", country: "United States", lat: 47.4489, lon: -122.3094 },
  { icao: "KMIA", iata: "MIA", name: "Miami Intl", city: "Miami", country: "United States", lat: 25.7932, lon: -80.2906 },
  { icao: "KLAS", iata: "LAS", name: "Harry Reid", city: "Las Vegas", country: "United States", lat: 36.084, lon: -115.1537 },
  { icao: "KBOS", iata: "BOS", name: "Logan", city: "Boston", country: "United States", lat: 42.3643, lon: -71.0052 },
  { icao: "CYYZ", iata: "YYZ", name: "Toronto Pearson", city: "Toronto", country: "Canada", lat: 43.6777, lon: -79.6248 },
  { icao: "CYVR", iata: "YVR", name: "Vancouver Intl", city: "Vancouver", country: "Canada", lat: 49.1939, lon: -123.184 },
  { icao: "MMMX", iata: "MEX", name: "Benito Juárez", city: "Mexico City", country: "Mexico", lat: 19.4363, lon: -99.0721 },

  // --- Central & South America ---
  { icao: "SBGR", iata: "GRU", name: "Guarulhos", city: "São Paulo", country: "Brazil", lat: -23.4356, lon: -46.4731 },
  { icao: "SAEZ", iata: "EZE", name: "Ezeiza", city: "Buenos Aires", country: "Argentina", lat: -34.8222, lon: -58.5358 },
  { icao: "SKBO", iata: "BOG", name: "El Dorado", city: "Bogotá", country: "Colombia", lat: 4.7016, lon: -74.1469 },
  { icao: "SCEL", iata: "SCL", name: "Arturo Merino Benítez", city: "Santiago", country: "Chile", lat: -33.393, lon: -70.7858 },
  { icao: "MPTO", iata: "PTY", name: "Tocumen", city: "Panama City", country: "Panama", lat: 9.0714, lon: -79.3835 },

  // --- Middle East ---
  { icao: "OMDB", iata: "DXB", name: "Dubai Intl", city: "Dubai", country: "United Arab Emirates", lat: 25.2528, lon: 55.3644 },
  { icao: "OMAA", iata: "AUH", name: "Zayed Intl", city: "Abu Dhabi", country: "United Arab Emirates", lat: 24.433, lon: 54.6511 },
  { icao: "OTHH", iata: "DOH", name: "Hamad Intl", city: "Doha", country: "Qatar", lat: 25.2731, lon: 51.6081 },
  { icao: "OEJN", iata: "JED", name: "King Abdulaziz", city: "Jeddah", country: "Saudi Arabia", lat: 21.6796, lon: 39.1565 },
  { icao: "LLBG", iata: "TLV", name: "Ben Gurion", city: "Tel Aviv", country: "Israel", lat: 32.0114, lon: 34.8867 },

  // --- Asia ---
  { icao: "RJTT", iata: "HND", name: "Haneda", city: "Tokyo", country: "Japan", lat: 35.5523, lon: 139.7798 },
  { icao: "RJAA", iata: "NRT", name: "Narita", city: "Tokyo", country: "Japan", lat: 35.7719, lon: 140.3928 },
  { icao: "RKSI", iata: "ICN", name: "Incheon", city: "Seoul", country: "South Korea", lat: 37.4602, lon: 126.4407 },
  { icao: "ZBAA", iata: "PEK", name: "Beijing Capital", city: "Beijing", country: "China", lat: 40.0801, lon: 116.5846 },
  { icao: "ZSPD", iata: "PVG", name: "Pudong", city: "Shanghai", country: "China", lat: 31.1434, lon: 121.8052 },
  { icao: "ZGGG", iata: "CAN", name: "Baiyun", city: "Guangzhou", country: "China", lat: 23.3924, lon: 113.2988 },
  { icao: "VHHH", iata: "HKG", name: "Hong Kong Intl", city: "Hong Kong", country: "Hong Kong", lat: 22.308, lon: 113.9185 },
  { icao: "RCTP", iata: "TPE", name: "Taoyuan", city: "Taipei", country: "Taiwan", lat: 25.0777, lon: 121.233 },
  { icao: "WSSS", iata: "SIN", name: "Changi", city: "Singapore", country: "Singapore", lat: 1.3644, lon: 103.9915 },
  { icao: "VTBS", iata: "BKK", name: "Suvarnabhumi", city: "Bangkok", country: "Thailand", lat: 13.69, lon: 100.7501 },
  { icao: "WMKK", iata: "KUL", name: "Kuala Lumpur Intl", city: "Kuala Lumpur", country: "Malaysia", lat: 2.7456, lon: 101.7099 },
  { icao: "WIII", iata: "CGK", name: "Soekarno-Hatta", city: "Jakarta", country: "Indonesia", lat: -6.1256, lon: 106.6559 },
  { icao: "RPLL", iata: "MNL", name: "Ninoy Aquino", city: "Manila", country: "Philippines", lat: 14.5086, lon: 121.0197 },
  { icao: "VIDP", iata: "DEL", name: "Indira Gandhi", city: "Delhi", country: "India", lat: 28.5562, lon: 77.1 },
  { icao: "VABB", iata: "BOM", name: "Chhatrapati Shivaji", city: "Mumbai", country: "India", lat: 19.0887, lon: 72.8679 },

  // --- Oceania ---
  { icao: "YSSY", iata: "SYD", name: "Kingsford Smith", city: "Sydney", country: "Australia", lat: -33.9461, lon: 151.1772 },
  { icao: "YMML", iata: "MEL", name: "Tullamarine", city: "Melbourne", country: "Australia", lat: -37.6733, lon: 144.8433 },
  { icao: "NZAA", iata: "AKL", name: "Auckland", city: "Auckland", country: "New Zealand", lat: -37.0082, lon: 174.785 },

  // --- Africa ---
  { icao: "FAOR", iata: "JNB", name: "O. R. Tambo", city: "Johannesburg", country: "South Africa", lat: -26.1392, lon: 28.246 },
  { icao: "HECA", iata: "CAI", name: "Cairo Intl", city: "Cairo", country: "Egypt", lat: 30.1219, lon: 31.4056 },
  { icao: "HAAB", iata: "ADD", name: "Bole", city: "Addis Ababa", country: "Ethiopia", lat: 8.9779, lon: 38.7993 },
  { icao: "GMMN", iata: "CMN", name: "Mohammed V", city: "Casablanca", country: "Morocco", lat: 33.3675, lon: -7.5899 },
  { icao: "DNMM", iata: "LOS", name: "Murtala Muhammed", city: "Lagos", country: "Nigeria", lat: 6.5774, lon: 3.3212 },
];
