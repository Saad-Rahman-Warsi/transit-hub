import type { Arrival, Stop } from '../types';
import ArrivalChip from './ArrivalChip';
import OccupancyBar from './OccupancyBar';

interface Props {
  stop: Stop | null;
  arrivals: Arrival[];
  loading: boolean;
  error: string | null;
  lastUpdated: Date | null;
}

// Colour map for common OC Transpo route numbers
const ROUTE_COLORS: Record<string, { bg: string; color: string }> = {
  '1':   { bg: '#E6F1FB', color: '#0C447C' },
  '2':   { bg: '#EEEDFE', color: '#26215C' },
  '4':   { bg: '#EAF3DE', color: '#173404' },
  '5':   { bg: '#FBEAF0', color: '#4B1528' },
  '7':   { bg: '#FBEAF0', color: '#4B1528' },
  '8':   { bg: '#FAEEDA', color: '#412402' },
  '14':  { bg: '#E1F5EE', color: '#04342C' },
  '16':  { bg: '#FAEEDA', color: '#412402' },
  '85':  { bg: '#E6F1FB', color: '#0C447C' },
  '95':  { bg: '#EEEDFE', color: '#26215C' },
  '97':  { bg: '#FAEEDA', color: '#633806' },
  '98':  { bg: '#EAF3DE', color: '#27500A' },
};

function routeBadge(routeId: string) {
  return ROUTE_COLORS[routeId] ?? { bg: 'var(--color-background-secondary)', color: 'var(--color-text-secondary)' };
}

function secondsToLabel(delay: number): string {
  if (Math.abs(delay) < 60) return 'On time';
  const mins = Math.round(Math.abs(delay) / 60);
  return delay > 0 ? `${mins} min late` : `${mins} min early`;
}

function delayColor(delay: number): string {
  if (Math.abs(delay) < 60) return '#1D9E75';
  if (delay > 0) return '#E24B4A';
  return '#378ADD';
}

export default function ArrivalsBoard({ stop, arrivals, loading, error, lastUpdated }: Props) {
  if (!stop) {
    return (
      <div
        style={{
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexDirection: 'column',
          gap: 12,
          color: 'var(--color-text-tertiary)',
        }}
      >
        <div style={{ fontSize: 32 }}>🚌</div>
        <p style={{ fontSize: 14 }}>Select a stop to see arrivals</p>
      </div>
    );
  }

  const secondsAgo = lastUpdated
    ? Math.round((Date.now() - lastUpdated.getTime()) / 1000)
    : null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', flex: 1, minHeight: 0 }}>
      {/* Header */}
      <div
        style={{
          padding: '16px 20px',
          background: 'var(--color-background-primary)',
          borderBottom: '0.5px solid var(--color-border-tertiary)',
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          gap: 16,
        }}
      >
        <div>
          <h2
            style={{
              fontSize: 17,
              fontWeight: 500,
              color: 'var(--color-text-primary)',
              margin: 0,
            }}
          >
            {stop.stopName}
          </h2>
          <div
            style={{
              fontSize: 13,
              color: 'var(--color-text-secondary)',
              marginTop: 2,
              display: 'flex',
              alignItems: 'center',
              gap: 8,
            }}
          >
            Stop #{stop.stopCode}
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 4,
                fontSize: 12,
                color: '#0F6E56',
              }}
            >
              <span
                style={{
                  width: 7,
                  height: 7,
                  borderRadius: '50%',
                  background: '#1D9E75',
                  animation: 'pulse 2s infinite',
                }}
              />
              Live
            </span>
          </div>
        </div>

        {secondsAgo != null && (
          <div style={{ fontSize: 12, color: 'var(--color-text-tertiary)', flexShrink: 0, paddingTop: 4 }}>
            Updated {secondsAgo}s ago
          </div>
        )}
      </div>

      {/* Stats row */}
      {arrivals.length > 0 && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: 10,
            padding: '14px 20px 0',
          }}
        >
          {[
            {
              label: 'Next arrival',
              value: arrivals[0].arrivalLabel,
              sub: arrivals[0].headsign,
            },
            {
              label: 'Routes serving stop',
              value: String(new Set(arrivals.map((a) => a.routeId)).size),
              sub: 'in next hour',
            },
            {
              label: 'On-time status',
              value: secondsToLabel(arrivals[0].delaySeconds),
              sub: arrivals[0].routeShortName,
              valueColor: delayColor(arrivals[0].delaySeconds),
            },
          ].map((card) => (
            <div
              key={card.label}
              style={{
                background: 'var(--color-background-secondary)',
                borderRadius: 8,
                padding: '10px 14px',
              }}
            >
              <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginBottom: 4 }}>
                {card.label}
              </div>
              <div
                style={{
                  fontSize: 18,
                  fontWeight: 500,
                  color: card.valueColor ?? 'var(--color-text-primary)',
                }}
              >
                {card.value}
              </div>
              <div style={{ fontSize: 11, color: 'var(--color-text-tertiary)', marginTop: 2 }}>
                {card.sub}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Arrivals list */}
      <div
        style={{
          flex: 1,
          overflowY: 'auto',
          padding: '14px 20px',
          display: 'flex',
          flexDirection: 'column',
          gap: 8,
        }}
      >
        {loading && arrivals.length === 0 && (
          <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-tertiary)', fontSize: 13 }}>
            Loading arrivals…
          </div>
        )}

        {error && (
          <div
            style={{
              padding: '12px 16px',
              background: '#FCEBEB',
              border: '0.5px solid #F7C1C1',
              borderRadius: 8,
              fontSize: 13,
              color: '#501313',
            }}
          >
            Could not load arrivals: {error}
          </div>
        )}

        {!loading && !error && arrivals.length === 0 && (
          <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-tertiary)', fontSize: 13 }}>
            No upcoming arrivals found for this stop.
          </div>
        )}

        {arrivals.map((arrival) => {
          const badge = routeBadge(arrival.routeShortName);
          return (
            <div
              key={`${arrival.tripId}-${arrival.scheduledArrivalEpoch}`}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                padding: '12px 16px',
                background: 'var(--color-background-primary)',
                border: '0.5px solid var(--color-border-tertiary)',
                borderRadius: 12,
                transition: 'border-color 0.15s',
              }}
              onMouseEnter={(e) =>
                ((e.currentTarget as HTMLElement).style.borderColor = 'var(--color-border-secondary)')
              }
              onMouseLeave={(e) =>
                ((e.currentTarget as HTMLElement).style.borderColor = 'var(--color-border-tertiary)')
              }
            >
              {/* Route badge */}
              <div
                style={{
                  padding: '3px 10px',
                  borderRadius: 6,
                  background: badge.bg,
                  color: badge.color,
                  fontSize: 13,
                  fontWeight: 500,
                  minWidth: 40,
                  textAlign: 'center',
                  flexShrink: 0,
                }}
              >
                {arrival.routeShortName}
              </div>

              {/* Destination */}
              <div style={{ flex: 1, minWidth: 0 }}>
                <div
                  style={{
                    fontSize: 14,
                    fontWeight: 500,
                    color: 'var(--color-text-primary)',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                  }}
                >
                  {arrival.headsign}
                </div>
                <div
                  style={{
                    fontSize: 12,
                    color: 'var(--color-text-secondary)',
                    marginTop: 2,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 6,
                  }}
                >
                  <span style={{ color: delayColor(arrival.delaySeconds) }}>
                    {secondsToLabel(arrival.delaySeconds)}
                  </span>
                  {arrival.vehicleId && (
                    <span style={{ color: 'var(--color-text-tertiary)' }}>· Bus #{arrival.vehicleId}</span>
                  )}
                </div>
              </div>

              {/* Occupancy */}
              <OccupancyBar status={arrival.occupancyStatus} />

              {/* Arrival times — show up to 3 */}
              <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
                <ArrivalChip arrival={arrival} />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
