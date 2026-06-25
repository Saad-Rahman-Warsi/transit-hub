import { useState, useEffect, useRef } from 'react';
import type { Stop } from './types';
import {
  useGeolocation,
  useNearbyStops,
  useStopSearch,
  useArrivals,
  useAlerts,
} from './hooks/useTransit';
import StopList from './components/StopList';
import ArrivalsBoard from './components/ArrivalsBoard';
import AlertBanner from './components/AlertBanner';
import MapView from './components/MapView';

type ActiveTab = 'arrivals' | 'map';

// Pulse animation keyframes injected once
const GLOBAL_STYLE = `
  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50%       { opacity: 0.35; }
  }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: var(--font-sans, system-ui, sans-serif); }
  ::-webkit-scrollbar { width: 6px; }
  ::-webkit-scrollbar-track { background: transparent; }
  ::-webkit-scrollbar-thumb { background: var(--color-border-secondary); border-radius: 3px; }
`;

export default function App() {
  const [query, setQuery] = useState('');
  const [selectedStop, setSelectedStop] = useState<Stop | null>(null);
  const [activeTab, setActiveTab] = useState<ActiveTab>('arrivals');
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [debouncedQuery, setDebouncedQuery] = useState('');

  // Geolocation
  const { coords } = useGeolocation();

  // Debounce search input
  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => setDebouncedQuery(query), 300);
    return () => { if (debounceRef.current) clearTimeout(debounceRef.current); };
  }, [query]);

  // Data hooks
  const nearby = useNearbyStops(coords?.lat, coords?.lon);
  const searched = useStopSearch(debouncedQuery, coords?.lat, coords?.lon);
  const { data: arrivals, loading: arrivalsLoading, error: arrivalsError, lastUpdated } =
    useArrivals(selectedStop?.stopCode ?? null);
  const { data: alerts } = useAlerts();

  // Stops to show in sidebar: search results override nearby
  const stopsToShow = debouncedQuery.length >= 2
    ? (searched.data ?? [])
    : (nearby.data ?? []);
  const stopsLoading = debouncedQuery.length >= 2 ? searched.loading : nearby.loading;

  // Auto-select first stop once nearby loads
  useEffect(() => {
    if (!selectedStop && stopsToShow.length > 0) {
      setSelectedStop(stopsToShow[0]);
    }
  }, [stopsToShow, selectedStop]);

  const handleStopSelect = (stop: Stop) => {
    setSelectedStop(stop);
    setActiveTab('arrivals');
  };

  const severeAlerts = (alerts ?? []).filter((a) => a.severity === 'SEVERE');
  const otherAlerts  = (alerts ?? []).filter((a) => a.severity !== 'SEVERE');

  return (
    <>
      <style>{GLOBAL_STYLE}</style>

      <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', background: 'var(--color-background-tertiary)' }}>

        {/* ── Top bar ── */}
        <header
          style={{
            background: 'var(--color-background-primary)',
            borderBottom: '0.5px solid var(--color-border-tertiary)',
            padding: '0 20px',
            display: 'flex',
            alignItems: 'center',
            gap: 16,
            height: 52,
            flexShrink: 0,
          }}
        >
          {/* Logo */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
            <div
              style={{
                width: 10,
                height: 10,
                borderRadius: '50%',
                background: '#1D9E75',
              }}
            />
            <span style={{ fontSize: 15, fontWeight: 500, color: 'var(--color-text-primary)' }}>
              Open Transit Hub
            </span>
          </div>

          {/* Search */}
          <div style={{ flex: 1, position: 'relative' }}>
            <span
              style={{
                position: 'absolute',
                left: 10,
                top: '50%',
                transform: 'translateY(-50%)',
                fontSize: 14,
                color: 'var(--color-text-tertiary)',
                pointerEvents: 'none',
              }}
            >
              ⌕
            </span>
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search stops, routes, intersections…"
              style={{
                width: '100%',
                height: 34,
                border: '0.5px solid var(--color-border-secondary)',
                borderRadius: 8,
                padding: '0 12px 0 34px',
                fontSize: 13,
                background: 'var(--color-background-secondary)',
                color: 'var(--color-text-primary)',
                outline: 'none',
              }}
            />
          </div>

          {/* Agency badge */}
          <div
            style={{
              fontSize: 12,
              padding: '4px 12px',
              borderRadius: 20,
              background: '#E1F5EE',
              color: '#0F6E56',
              border: '0.5px solid #5DCAA5',
              flexShrink: 0,
            }}
          >
            OC Transpo · GTFS-RT
          </div>
        </header>

        {/* ── Severe alert strip ── */}
        {severeAlerts.length > 0 && (
          <div style={{ padding: '6px 20px', background: '#FCEBEB', borderBottom: '0.5px solid #F7C1C1' }}>
            {severeAlerts.slice(0, 2).map((a) => (
              <AlertBanner key={a.id} alert={a} />
            ))}
          </div>
        )}

        {/* ── Body ── */}
        <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>

          {/* ── Sidebar ── */}
          <aside
            style={{
              width: 264,
              flexShrink: 0,
              background: 'var(--color-background-primary)',
              borderRight: '0.5px solid var(--color-border-tertiary)',
              display: 'flex',
              flexDirection: 'column',
              overflow: 'hidden',
            }}
          >
            {/* Alerts section */}
            {otherAlerts.length > 0 && (
              <div style={{ padding: '12px 12px 0' }}>
                <div
                  style={{
                    fontSize: 11,
                    fontWeight: 500,
                    color: 'var(--color-text-tertiary)',
                    textTransform: 'uppercase',
                    letterSpacing: '0.06em',
                    marginBottom: 8,
                  }}
                >
                  Service alerts
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 12 }}>
                  {otherAlerts.slice(0, 3).map((a) => (
                    <AlertBanner key={a.id} alert={a} />
                  ))}
                </div>
              </div>
            )}

            {/* Stop list */}
            <div style={{ padding: '12px 12px 0' }}>
              <div
                style={{
                  fontSize: 11,
                  fontWeight: 500,
                  color: 'var(--color-text-tertiary)',
                  textTransform: 'uppercase',
                  letterSpacing: '0.06em',
                  marginBottom: 8,
                }}
              >
                {debouncedQuery.length >= 2 ? 'Search results' : 'Nearby stops'}
              </div>
            </div>
            <div style={{ flex: 1, overflowY: 'auto', padding: '0 12px 12px' }}>
              <StopList
                stops={stopsToShow}
                selectedCode={selectedStop?.stopCode ?? null}
                onSelect={handleStopSelect}
                loading={stopsLoading}
              />
            </div>
          </aside>

          {/* ── Main panel ── */}
          <main style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            {/* Tabs */}
            <div
              style={{
                background: 'var(--color-background-primary)',
                borderBottom: '0.5px solid var(--color-border-tertiary)',
                display: 'flex',
                padding: '0 20px',
                flexShrink: 0,
              }}
            >
              {(['arrivals', 'map'] as ActiveTab[]).map((tab) => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab)}
                  style={{
                    fontSize: 13,
                    padding: '10px 14px',
                    cursor: 'pointer',
                    color: activeTab === tab ? '#0F6E56' : 'var(--color-text-secondary)',
                    borderBottom: activeTab === tab ? '2px solid #1D9E75' : '2px solid transparent',
                    fontWeight: activeTab === tab ? 500 : 400,
                    background: 'transparent',
                    border: 'none',
                    borderBottom: activeTab === tab ? '2px solid #1D9E75' : '2px solid transparent',
                    marginBottom: -1,
                    textTransform: 'capitalize',
                  }}
                >
                  {tab}
                </button>
              ))}
            </div>

            {/* Tab content */}
            {activeTab === 'arrivals' ? (
              <ArrivalsBoard
                stop={selectedStop}
                arrivals={arrivals ?? []}
                loading={arrivalsLoading}
                error={arrivalsError}
                lastUpdated={lastUpdated}
              />
            ) : (
              <MapView
                selectedStop={selectedStop}
                nearbyStops={stopsToShow}
                onStopClick={handleStopSelect}
              />
            )}
          </main>
        </div>
      </div>
    </>
  );
}
