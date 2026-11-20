import type { EmergencyTier } from "../../api/types";

const labels: Record<EmergencyTier, string> = {
  NORMAL: "Normal",
  MEDIUM: "Medium",
  HIGH: "High",
  CRITICAL: "Critical",
};

export function PriorityBadge({ tier }: { tier: EmergencyTier }) {
  return (
    <span className={`priority-badge priority-${tier.toLowerCase()}`} data-priority={tier}>
      {labels[tier]}
    </span>
  );
}
