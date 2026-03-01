const LABEL_MAP: Record<string, string> = {
  ANALYST: "Analyst",
  RISK_LEAD: "Risk Lead",
  ADMIN: "Admin",
  ACTIVE: "Active",
  CLOSED: "Closed",
  CURRENT_RATIO: "Current Ratio",
  DEBT_TO_EQUITY: "Debt to Equity",
  DSCR: "DSCR",
  INTEREST_COVERAGE: "Interest Coverage",
  TANGIBLE_NET_WORTH: "Tangible Net Worth",
  DEBT_TO_EBITDA: "Debt to EBITDA",
  FIXED_CHARGE_COVERAGE: "Fixed Charge Coverage",
  QUICK_RATIO: "Quick Ratio",
  GREATER_THAN_EQUAL: "Greater Than or Equal",
  LESS_THAN_EQUAL: "Less Than or Equal",
  PASS: "Pass",
  BREACH: "Breach",
  OPEN: "Open",
  ACKNOWLEDGED: "Acknowledged",
  UNDER_REVIEW: "Under Review",
  RESOLVED: "Resolved",
  EARLY_WARNING: "Early Warning",
  HIGH: "High",
  MEDIUM: "Medium",
  LOW: "Low",
  QUARTERLY: "Quarterly",
  ANNUAL: "Annual",
};

const ACRONYMS = new Set(["ID", "CSV", "XLSX", "DSCR", "EBITDA", "EBIT"]);

export function formatEnumLabel(value: string | null | undefined) {
  if (!value) return "-";
  const trimmed = value.trim();
  if (!trimmed) return "-";
  const direct = LABEL_MAP[trimmed.toUpperCase()];
  if (direct) return direct;

  return trimmed
    .split("_")
    .map((segment) => segment.trim())
    .filter(Boolean)
    .map((segment) => {
      const upper = segment.toUpperCase();
      if (ACRONYMS.has(upper)) return upper;
      return `${segment[0].toUpperCase()}${segment.slice(1).toLowerCase()}`;
    })
    .join(" ");
}

export function formatAlertMessage(message: string) {
  const base = message.replace(/_/g, " ").replace(/\s+/g, " ").trim();
  if (!base) return "Alert requires attention.";
  const lowered = base.toLowerCase();
  return `${lowered[0].toUpperCase()}${lowered.slice(1)}`;
}

export function formatDateTime(value: string | null | undefined) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat(undefined, {
    year: "numeric",
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}
