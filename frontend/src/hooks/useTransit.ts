import { useCallback, useEffect, useRef, useState } from 'react';
import type { Arrival, ServiceAlert, Stop, VehiclePosition } from '../types';
import * as api from '../services/api';

// ── Generic polling hook ──────────────────────────────────────────────────────
function usePolling<T>(
  fetcher: () => Promise<T>,
  intervalMs: number,
  deps: unknown[] = []
): { data: T | null; loading: boolean; error: string | null; lastUpdated: Date | null } {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const fetch = useCallback(async () => {
    try {
      const result = await fetcher();
      setData(result);
      setLastUpdated(new Date());
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  }, deps); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    setLoading(true);
    fetch();
    timerRef.current = setInterval(fetch, intervalMs);
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, [fetch, intervalMs]);

  return { data, loading, error, lastUpdated };
}

// ── Public hooks ──────────────────────────────────────────────────────────────

export function useNearbyStops(lat?: number, lon?: number) {
  return usePolling<Stop[]>(
    () =>
      lat != null && lon != null
        ? api.getNearbyStops(lat, lon, 600, 10)
        : Promise.resolve([]),
    60_000,
    [lat, lon]
  );
}

export function useStopSearch(query: string, lat?: number, lon?: number) {
  return usePolling<Stop[]>(
    () => (query.length >= 2 ? api.searchStops(query, lat, lon) : Promise.resolve([])),
    30_000,
    [query, lat, lon]
  );
}

export function useArrivals(stopCode: string | null) {
  return usePolling<Arrival[]>(
    () => (stopCode ? api.getArrivals(stopCode) : Promise.resolve([])),
    30_000,      // GTFS-RT TripUpdates refresh every 30 s
    [stopCode]
  );
}

export function useVehicles(route?: string) {
  return usePolling<VehiclePosition[]>(
    () => api.getVehicles(route),
    30_000,
    [route]
  );
}

export function useAlerts(route?: string) {
  return usePolling<ServiceAlert[]>(
    () => api.getAlerts(route),
    300_000,     // alerts refresh every 5 minutes
    [route]
  );
}

// ── Geolocation ───────────────────────────────────────────────────────────────
export function useGeolocation() {
  const [coords, setCoords] = useState<{ lat: number; lon: number } | null>(null);
  const [denied, setDenied] = useState(false);

  useEffect(() => {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(
      (pos) => setCoords({ lat: pos.coords.latitude, lon: pos.coords.longitude }),
      () => setDenied(true),
      { timeout: 8000 }
    );
  }, []);

  return { coords, denied };
}
