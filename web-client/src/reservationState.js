export const studentId = 'demo-student'
export const cancellationReasons = ['Nu mai ajung la timp', 'S-a stricat mașina', 'Motive personale']
export const endTime = (booking) => new Date(`${booking.date}T${String(booking.hour).padStart(2, '0')}:00`).getTime() + 3600000
export const isActive = (booking, now = Date.now()) => booking.status !== 'cancelled' && endTime(booking) > now
export const isMine = (booking) => !booking.ownerId || booking.ownerId === studentId

export function changeReservation(bookings, id, change, now = Date.now()) {
  const booking = bookings.find((item) => item.id === id)
  if (!booking || !isMine(booking) || !isActive(booking, now)) throw new Error('Rezervarea nu mai este activă. Reîncarcă lista și încearcă din nou.')
  if (change.kind === 'cancel') {
    if (change.reason && !cancellationReasons.includes(change.reason)) throw new Error('Selectează un motiv valid.')
    return bookings.map((item) => item.id === id ? { ...item, status: 'cancelled', reason: change.reason, cancelledAt: new Date(now).toISOString() } : item)
  }
  const start = new Date(`${change.date}T${String(change.hour).padStart(2, '0')}:00`).getTime()
  if (!Number.isInteger(change.hour) || change.hour < 0 || change.hour > 23 || !Number.isFinite(start) || start <= now) throw new Error('Alege un interval viitor valid.')
  if (booking.date === change.date && booking.hour === change.hour) throw new Error('Alege un interval diferit de rezervarea actuală.')
  if (bookings.some((item) => item.id !== id && item.machineId === booking.machineId && isActive(item, now) && item.date === change.date && item.hour === change.hour)) throw new Error('Intervalul a fost rezervat între timp. Alege altă oră.')
  return bookings.map((item) => item.id === id ? { ...item, date: change.date, hour: change.hour, moves: [...(item.moves || []), { date: item.date, hour: item.hour, movedAt: new Date(now).toISOString() }] } : item)
}
