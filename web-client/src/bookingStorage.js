import { equipment } from './equipment'
import { endTime } from './reservationState'

export const storageKey = 'laundry-bookings-v1'

export function readBookings() {
  try {
    const saved = JSON.parse(localStorage.getItem(storageKey) || '[]')
    return Array.isArray(saved) ? saved.filter((item) =>
      item && equipment.some((machine) => machine.id === item.machineId) &&
      /^\d{4}-\d{2}-\d{2}$/.test(item.date) &&
      Number.isInteger(item.hour) && item.hour >= 0 && item.hour < 24 &&
      Number.isFinite(endTime(item)),
    ) : []
  } catch { return [] }
}
