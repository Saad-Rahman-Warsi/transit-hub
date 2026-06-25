// ── Stop ──────────────────────────────────────────────────────────────────────
export interface Stop {
  stopId: string;
  stopCode: string;
  stopName: string;
  lat: number;
  lon: number;
  distanceMetres?: number;
  routeCount?: number;
}

// ── Arrival ───────────────────────────────────────────────────────────────────
export interface Arrival {
  routeId: string;
  routeShortName: string;
  routeLongName?: string;
  headsign: string;
  tripId: string;
  scheduledArrivalEpoch: number;
  predictedArrivalEpoch: number;
  delaySeconds: number;
  isRealTime: boolean;
  vehicleId?: string;
  occupancyStatus: OccupancyStatus;
  minutesUntilArrival: number;
  arrivalLabel: string;
}

export type OccupancyStatus =
  | 'EMPTY'
  | 'MANY_SEATS_AVAILABLE'
  | 'FEW_SEATS_AVAILABLE'
  | 'STANDING_ROOM_ONLY'
  | 'CRUSHED_STANDING_ROOM_ONLY'
  | 'FULL'
  | 'UNKNOWN';

// ── VehiclePosition ───────────────────────────────────────────────────────────
export interface VehiclePosition {
  vehicleId: string;
  routeId?: string;
  tripId?: string;
  label?: string;
  latitude: number;
  longitude: number;
  bearing?: number;
  speed?: number;
  timestamp: number;
  occupancyStatus: OccupancyStatus;
  currentStopId?: string;
  currentStatus?: string;
}

// ── ServiceAlert ──────────────────────────────────────────────────────────────
export interface ServiceAlert {
  id: string;
  title: string;
  description: string;
  severity: 'INFO' | 'WARNING' | 'SEVERE';
  affectedRoutes: string[];
  publishedAt: string;
  link: string;
}
