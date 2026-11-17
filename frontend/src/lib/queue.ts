import type { EmergencyTier, QueueEntry } from "../api/types";

export const priorityRank: Record<EmergencyTier, number> = {
  CRITICAL: 0,
  HIGH: 1,
  MEDIUM: 2,
  NORMAL: 3,
};

export function compareQueueEntries(left: QueueEntry, right: QueueEntry): number {
  const priority = priorityRank[left.triageLevel] - priorityRank[right.triageLevel];
  if (priority !== 0) {
    return priority;
  }
  return left.arrivedAt.localeCompare(right.arrivedAt);
}

export function sortQueueEntries(entries: QueueEntry[]): QueueEntry[] {
  return [...entries].sort(compareQueueEntries);
}
