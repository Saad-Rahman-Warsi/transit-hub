import type { OccupancyStatus } from '../types';

const LEVELS: Record<OccupancyStatus, number> = {
  EMPTY: 5,
  MANY_SEATS_AVAILABLE: 30,
  FEW_SEATS_AVAILABLE: 60,
  STANDING_ROOM_ONLY: 80,
  CRUSHED_STANDING_ROOM_ONLY: 95,
  FULL: 100,
  UNKNOWN: 0,
};

const COLORS: Record<OccupancyStatus, string> = {
  EMPTY: '#1D9E75',
  MANY_SEATS_AVAILABLE: '#1D9E75',
  FEW_SEATS_AVAILABLE: '#EF9F27',
  STANDING_ROOM_ONLY: '#EF9F27',
  CRUSHED_STANDING_ROOM_ONLY: '#E24B4A',
  FULL: '#E24B4A',
  UNKNOWN: '#888780',
};

const LABELS: Record<OccupancyStatus, string> = {
  EMPTY: 'Empty',
  MANY_SEATS_AVAILABLE: 'Seats available',
  FEW_SEATS_AVAILABLE: 'Few seats',
  STANDING_ROOM_ONLY: 'Standing only',
  CRUSHED_STANDING_ROOM_ONLY: 'Very crowded',
  FULL: 'Full',
  UNKNOWN: '',
};

interface Props {
  status: OccupancyStatus;
  showLabel?: boolean;
}

export default function OccupancyBar({ status, showLabel = false }: Props) {
  const pct = LEVELS[status] ?? 0;
  const color = COLORS[status] ?? '#888780';
  const label = LABELS[status] ?? '';

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <div
        title={label}
        style={{
          width: 44,
          height: 4,
          borderRadius: 2,
          background: 'var(--color-border-tertiary)',
          overflow: 'hidden',
        }}
      >
        <div
          style={{
            width: `${pct}%`,
            height: '100%',
            background: color,
            borderRadius: 2,
            transition: 'width 0.4s ease',
          }}
        />
      </div>
      {showLabel && label && (
        <span style={{ fontSize: 11, color: 'var(--color-text-tertiary)' }}>{label}</span>
      )}
    </div>
  );
}
