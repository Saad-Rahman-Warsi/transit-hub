import type { Stop } from '../types';

interface Props {
  stops: Stop[];
  selectedCode: string | null;
  onSelect: (stop: Stop) => void;
  loading: boolean;
}

function formatDist(m?: number): string {
  if (m == null) return '';
  if (m < 1000) return `${Math.round(m)}m`;
  return `${(m / 1000).toFixed(1)}km`;
}

export default function StopList({ stops, selectedCode, onSelect, loading }: Props) {
  if (loading) {
    return (
      <div style={{ padding: '20px 0', textAlign: 'center', color: 'var(--color-text-tertiary)', fontSize: 13 }}>
        Finding stops…
      </div>
    );
  }

  if (stops.length === 0) {
    return (
      <div style={{ padding: '20px 0', textAlign: 'center', color: 'var(--color-text-tertiary)', fontSize: 13 }}>
        No stops found
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {stops.map((stop) => {
        const active = stop.stopCode === selectedCode;
        return (
          <button
            key={stop.stopId}
            onClick={() => onSelect(stop)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              padding: '8px 10px',
              borderRadius: 8,
              cursor: 'pointer',
              border: `0.5px solid ${active ? '#9FE1CB' : 'transparent'}`,
              background: active ? '#E1F5EE' : 'transparent',
              textAlign: 'left',
              width: '100%',
              transition: 'background 0.15s',
            }}
            onMouseEnter={(e) => {
              if (!active) (e.currentTarget as HTMLElement).style.background = 'var(--color-background-secondary)';
            }}
            onMouseLeave={(e) => {
              if (!active) (e.currentTarget as HTMLElement).style.background = 'transparent';
            }}
          >
            {/* Badge */}
            <div
              style={{
                width: 32,
                height: 32,
                borderRadius: 8,
                background: '#E6F1FB',
                color: '#0C447C',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 10,
                fontWeight: 500,
                flexShrink: 0,
              }}
            >
              {stop.stopCode || 'BUS'}
            </div>

            {/* Info */}
            <div style={{ flex: 1, minWidth: 0 }}>
              <div
                style={{
                  fontSize: 13,
                  fontWeight: 500,
                  color: 'var(--color-text-primary)',
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                }}
              >
                {stop.stopName}
              </div>
              <div style={{ fontSize: 11, color: 'var(--color-text-tertiary)' }}>
                #{stop.stopCode}
                {stop.routeCount ? ` · ${stop.routeCount} routes` : ''}
              </div>
            </div>

            {/* Distance */}
            {stop.distanceMetres != null && (
              <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', flexShrink: 0 }}>
                {formatDist(stop.distanceMetres)}
              </div>
            )}
          </button>
        );
      })}
    </div>
  );
}
