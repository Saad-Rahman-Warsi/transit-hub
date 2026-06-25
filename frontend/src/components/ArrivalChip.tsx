import type { Arrival } from '../types';

interface Props {
  arrival: Arrival;
}

function chipStyle(minutes: number): { background: string; color: string } {
  if (minutes <= 1)  return { background: '#E1F5EE', color: '#085041' };
  if (minutes <= 10) return { background: '#EAF3DE', color: '#173404' };
  return { background: 'var(--color-background-secondary)', color: 'var(--color-text-secondary)' };
}

export default function ArrivalChip({ arrival }: Props) {
  const style = chipStyle(arrival.minutesUntilArrival);
  return (
    <span
      title={arrival.isRealTime ? 'Real-time prediction' : 'Scheduled time'}
      style={{
        ...style,
        padding: '4px 10px',
        borderRadius: 20,
        fontSize: 13,
        fontWeight: 500,
        display: 'inline-flex',
        alignItems: 'center',
        gap: 4,
        whiteSpace: 'nowrap',
      }}
    >
      {arrival.arrivalLabel}
      {arrival.isRealTime && (
        <span
          style={{
            width: 6,
            height: 6,
            borderRadius: '50%',
            background: '#1D9E75',
            flexShrink: 0,
          }}
        />
      )}
    </span>
  );
}
