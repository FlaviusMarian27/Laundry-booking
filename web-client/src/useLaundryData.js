import { useEffect, useState } from 'react'
import { equipment } from './equipment'
import { readBookings } from './bookingStorage'
import { readProfile } from './profileStorage'
import { isActive, isMine } from './reservationState'

function snapshot() {
  const saved = readBookings()
  const profile = readProfile()
  const fullName = [profile.firstName, profile.lastName].filter(Boolean).join(' ') || 'Student'
  return {
    machines: equipment.map((machine) => ({ ...machine, status: saved.some((booking) => booking.machineId === machine.id && isActive(booking)) ? 'Ocupată' : machine.status })),
    bookings: saved.map((booking) => ({
      ...booking,
      student: isMine(booking) ? fullName : booking.student || 'Student',
      machine: equipment.find((machine) => machine.id === booking.machineId)?.name,
      time: `${booking.date} · ${String(booking.hour).padStart(2, '0')}:00 – ${String(booking.hour + 1).padStart(2, '0')}:00`,
      status: booking.status === 'cancelled' ? 'Anulată' : isActive(booking) ? 'Activă' : 'Încheiată',
    })),
  }
}

export default function useLaundryData() {
  const [data, setData] = useState(snapshot)
  useEffect(() => {
    const refresh = () => setData(snapshot())
    window.addEventListener('storage', refresh)
    window.addEventListener('focus', refresh)
    const timer = window.setInterval(refresh, 30000)
    return () => {
      window.removeEventListener('storage', refresh)
      window.removeEventListener('focus', refresh)
      window.clearInterval(timer)
    }
  }, [])
  return data
}
