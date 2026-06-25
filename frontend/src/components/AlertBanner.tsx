import type { ServiceAlert } from '../types';

interface Props {
  alert: ServiceAlert;
}

const SEVERITY_STYLES = {
  SEVERE: {
    bg: '#FCEBEB',
    border: '#F7C1C1',
    dot: '#E24B4A',
    text: '#501313',
  },
  WARNING: {
    bg: '#FAEEDA',
    border: '#FAC775',
    dot: '#EF9F27',
    text: '#412402',
  },
  INFO: {
    bg: '#E6F1FB',
    border: '#B5D4F4',
    dot: '#378ADD',
    text: '#042C53',
  },
};

export default function AlertBanner({ alert }: Props) {
  const s = SEVERITY_STYLES[alert.severity] ?? SEVERITY_STYLES.INFO;

  return (
    <a
      href={alert.link || '#'}
      target="_blank"
      rel="noopener noreferrer"
      style={{
        display: 'flex',
        gap: 10,
        alignItems: 'flex-start',
        padding: '8px 12px',
        background: s.bg,
        border: `0.5px solid ${s.border}`,
        borderRadius: 8,
        textDecoration: 'none',
        cursor: alert.link ? 'pointer' : 'default',
      }}
    >
      <span
        style={{
          width: 8,
          height: 8,
          borderRadius: '50%',
          background: s.dot,
          flexShrink: 0,
          marginTop: 4,
        }}
      />
      <div style={{ minWidth: 0 }}>
        <p
          style={{
            fontSize: 13,
            fontWeight: 500,
            color: s.text,
            margin: 0,
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
          }}
        >
          {alert.title}
        </p>
        {alert.description && (
          <p
            style={{
              fontSize: 12,
              color: s.text,
              margin: '2px 0 0',
              opacity: 0.8,
              lineHeight: 1.4,
            }}
          >
            {alert.description.slice(0, 120)}
            {alert.description.length > 120 ? '…' : ''}
          </p>
        )}
      </div>
    </a>
  );
}
