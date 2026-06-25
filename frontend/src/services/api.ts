import type { Arrival, ServiceAlert, Stop, VehiclePosition } from '../types';

const BASE = import.meta.env.VITE_API_BASE_URL ?? '/api';

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`);
  if (!res.ok) throw new Error(`API error ${res.status} on ${path}`);
  return res.json() as Promise<T>;
}

// ── Stops ─────────────────────────────────────────────────────────────────────

export function searchStops(
  query: string,
  lat?: number,
  lon?: number,
  limit = 20
): Promise<Stop[]> {
  const params = new URLSearchParams({ q: query, limit: String(limit) });
  if (lat != null) params.set('lat', String(lat));
  if (lon != null) params.set('lon', String(lon));
  return get<Stop[]>(`/stops/search?${params}`);
}

export function getNearbyStops(
  lat: number,
  lon: number,
  radius = 500,
  limit = 10
): Promise<Stop[]> {
  const params = new URLSearchParams({
    lat: String(lat),
    lon: String(lon),
    radius: String(radius),
    limit: String(limit),
  });
  return get<Stop[]>(`/stops/nearby?${params}`);
}

export function getStop(code: string): Promise<Stop> {
  return get<Stop>(`/stops/${code}`);
}

// ── Arrivals ──────────────────────────────────────────────────────────────────

export function getArrivals(stopCode: string, limit = 12): Promise<Arrival[]> {
  return get<Arrival[]>(`/arrivals/${stopCode}?limit=${limit}`);
}

// ── Vehicles ──────────────────────────────────────────────────────────────────

export function getVehicles(route?: string): Promise<VehiclePosition[]> {
  const params = route ? `?route=${route}` : '';
  return get<VehiclePosition[]>(`/vehicles${params}`);
}

// ── Alerts ────────────────────────────────────────────────────────────────────

export function getAlerts(route?: string): Promise<ServiceAlert[]> {
  const params = route ? `?route=${route}` : '';
  return get<ServiceAlert[]>(`/alerts${params}`);
}
