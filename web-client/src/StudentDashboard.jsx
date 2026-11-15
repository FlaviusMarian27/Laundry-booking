import { useEffect, useState } from 'react'
import './StudentDashboard.css'

import { equipment } from './equipment'
import { storageKey, readBookings } from './bookingStorage'
import { cancellationReasons, changeReservation, isActive, isMine, studentId } from './reservationState'
import StudentProfile from './StudentProfile'
import { readProfile } from './profileStorage'


const dateKey = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
const hourLabel = (hour) => `${String(hour).padStart(2, '0')}:00`
const dateLabel = (value) => new Date(`${value}T12:00:00`).toLocaleDateString('ro-RO', { day: 'numeric', month: 'long', year: 'numeric' })
const endsAt = (booking) => new Date(`${booking.date}T${hourLabel(booking.hour)}`).getTime() + 3600000

export default function StudentDashboard({ onLogout }) {
  const [allBookings, setBookings] = useState(readBookings)
  const bookings = allBookings.filter((booking) => isActive(booking))
  const [page, setPage] = useState('equipment')
  const [profile, setProfile] = useState(readProfile)
  const [moving, setMoving] = useState(null)
  const [selectedMachine, setSelectedMachine] = useState(null)
  const [notice, setNotice] = useState('')

  function navigate(next) {
    setPage(next)
    setSelectedMachine(null)
    setMoving(null)
    setNotice('')
  }

  function saveProfile(next) {
    localStorage.setItem('laundry-profile-v1', JSON.stringify(next))
    setProfile(next)
  }

  async function updateReservation(id, change) {
    const save = () => {
      const next = changeReservation(readBookings(), id, change)
      localStorage.setItem(storageKey, JSON.stringify(next))
      setBookings(next)
    }
    if (navigator.locks) await navigator.locks.request(storageKey, save)
    else save()
  }

  async function cancel(id, reason) {
    await updateReservation(id, { kind: 'cancel', reason })
    setNotice('Rezervarea a fost anulată. Echipamentul este disponibil din nou.')
  }

  function startMove(booking) {
    setMoving(booking)
    setSelectedMachine(equipment.find((machine) => machine.id === booking.machineId))
    setPage('equipment')
    setNotice('')
  }

  async function moveReservation(machine, date, hour) {
    await updateReservation(moving.id, { kind: 'move', date, hour })
    navigate('bookings')
    setNotice(`${machine.name}: rezervarea a fost mutată pentru ${dateLabel(date)}, ${hourLabel(hour)} – ${hourLabel(hour + 1)}.`)
  }

  useEffect(() => {
    const refresh = () => setBookings(readBookings())
    window.addEventListener('storage', refresh)
    const timer = window.setInterval(refresh, 30000)
    return () => { window.removeEventListener('storage', refresh); window.clearInterval(timer) }
  }, [])

  async function reserve(machine, date, hour) {
    const save = () => {
      const current = readBookings()
      if (current.some((booking) => booking.machineId === machine.id && isActive(booking))) {
        setBookings(current)
        throw new Error('Acest echipament tocmai a fost rezervat. Alege alt echipament.')
      }
      if (new Date(`${date}T${hourLabel(hour)}`).getTime() <= Date.now()) {
        throw new Error('Intervalul ales a trecut. Selectează o oră viitoare.')
      }
      const next = [...current, { id: crypto.randomUUID(), machineId: machine.id, date, hour, ownerId: studentId }]
      try {
        localStorage.setItem(storageKey, JSON.stringify(next))
      } catch {
        throw new Error('Rezervarea nu a putut fi salvată. Verifică permisiunile browserului și încearcă din nou.')
      }
      setBookings(next)
      setSelectedMachine(null)
      setNotice(`${machine.name}: rezervare confirmată pentru ${dateLabel(date)}, ${hourLabel(hour)} – ${hourLabel(hour + 1)}.`)
    }
    if (navigator.locks) await navigator.locks.request(storageKey, save)
    else save()
  }

  return (
    <div className="app student-app">
      <header className="navbar">
        <h2>🧺 Laundry Booking</h2>
        <ProfileMenu profile={profile} onNavigate={navigate} />
      </header>
      {notice && page !== 'equipment' && <div className="dashboard"><div className="booking-notice" role="status">{notice}</div></div>}
      {page === 'profile' ? <StudentProfile profile={profile} onSave={saveProfile} onLogout={onLogout} onBack={() => navigate('equipment')} /> : page === 'bookings' || page === 'history' ? <ReservationList bookings={allBookings} history={page === 'history'} onBack={() => navigate('equipment')} onCancel={cancel} onMove={startMove} /> : selectedMachine ? (
        <BookingPage key={`${selectedMachine.id}-${moving?.id || 'new'}`} machine={selectedMachine} bookings={bookings} moving={moving} onBack={() => { setSelectedMachine(null); if (moving) navigate('bookings') }} onReserve={moving ? moveReservation : reserve} />
      ) : (
        <main className="dashboard student-dashboard">
          <div className="student-intro"><span className="eyebrow">SPĂLĂTORIA CĂMINULUI</span><h1>Haine curate. Mai mult timp pentru tine.</h1><p>Alege echipamentul, ziua și ora. De restul ne ocupăm noi.</p></div>
          {notice && <div className="booking-notice" role="status">✓ {notice}</div>}
          {['Spălare', 'Uscare'].map((type) => (
            <section className="equipment-section" key={type}>
              <div className="equipment-heading"><div><span className="eyebrow">{type === 'Spălare' ? '01 / SPĂLARE' : '02 / USCARE'}</span><h2>{type === 'Spălare' ? 'Mașini de spălat' : 'Uscătoare'}</h2></div><span className="availability-count">{equipment.filter((machine) => machine.type === type && !bookings.some((booking) => booking.machineId === machine.id)).length} / {equipment.filter((machine) => machine.type === type).length} disponibile</span></div>
              <div className="machines-grid student-machines">
                {equipment.filter((machine) => machine.type === type).map((machine) => {
                  const booking = bookings.find((item) => item.machineId === machine.id)
                  return (
                    <article className={`machine-card equipment-card ${booking ? 'is-booked' : ''}`} key={machine.id}>
                      <div className={`appliance-art ${type === 'Uscare' ? 'dryer-art' : ''}`} aria-hidden="true"><div className="appliance-controls"><i /><i /><b /></div><div className="appliance-drum">{type === 'Uscare' ? '≋' : '≈'}</div></div>
                      <h3>{machine.name}</h3><p className="muted">{type} · ciclu de 60 minute</p>
                      <span className={`status-badge ${booking ? 'warning' : 'success'}`}>● {booking ? 'Ocupată' : 'Disponibilă'}</span>
                      {booking && <p className="reserved-interval">{dateLabel(booking.date)}<br />{hourLabel(booking.hour)} – {hourLabel(booking.hour + 1)}</p>}
                      <button className="primary-btn" disabled={Boolean(booking)} onClick={() => { setNotice(''); setSelectedMachine(machine) }}>{booking ? 'Deja rezervată' : 'Rezervă →'}</button>
                    </article>
                  )
                })}
              </div>
            </section>
          ))}
        </main>
      )}
    </div>
  )
}

function ProfileMenu({ profile, onNavigate }) {
  const [open, setOpen] = useState(false)
  useEffect(() => {
    if (!open) return
    const close = (event) => { if (!event.target.closest('.student-profile-menu')) setOpen(false) }
    const escape = (event) => { if (event.key === 'Escape') setOpen(false) }
    document.addEventListener('click', close)
    document.addEventListener('keydown', escape)
    return () => { document.removeEventListener('click', close); document.removeEventListener('keydown', escape) }
  }, [open])
  return <div className="student-profile-menu"><button className="profile-trigger" aria-expanded={open} aria-controls="profile-links" onClick={() => setOpen(!open)}><span>Bună, {profile.firstName || profile.lastName || 'Student'}!</span>{profile.photo ? <img className="profile-avatar" src={profile.photo} alt="Profil" /> : <span className="profile-avatar" aria-hidden="true">🎓</span>}</button>{open && <nav id="profile-links" className="profile-dropdown" aria-label="Cont student">{[['profile', 'Profil'], ['bookings', 'Rezervările tale'], ['history', 'Istoric']].map(([page, label]) => <button key={page} onClick={() => { onNavigate(page); setOpen(false) }}>{label} <span aria-hidden="true">→</span></button>)}</nav>}</div>
}

function ReservationList({ bookings, history, onBack, onCancel, onMove }) {
  const [cancelling, setCancelling] = useState(null)
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const mine = bookings.filter((booking) => isMine(booking) && (history ? !isActive(booking) : isActive(booking)))
    .sort((a, b) => history ? endsAt(b) - endsAt(a) : endsAt(a) - endsAt(b))
  async function confirmCancellation(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try { await onCancel(cancelling, reason); setCancelling(null) }
    catch (failure) { setError(failure.message) }
    finally { setBusy(false) }
  }
  return <main className="dashboard reservations-page"><button className="back-button" onClick={onBack}>← Înapoi la echipamente</button><span className="eyebrow">TIMPUL TĂU, ORGANIZAT</span><h1>{history ? 'Istoricul rezervărilor' : 'Rezervările tale'}</h1><p className="muted">{history ? 'Rezervările încheiate și anulate, într-un singur loc.' : 'Toate rezervările active pentru spălare și uscare.'}</p>{mine.length === 0 && <div className="reservation-empty"><span aria-hidden="true">📅</span><h2>{history ? 'Nicio rezervare în istoric' : 'Nu ai rezervări active'}</h2><p>Alege o mașină de spălat sau un uscător și rezervă un interval.</p><button className="primary-btn" onClick={onBack}>Vezi echipamentele →</button></div>}<div className="reservation-list">{mine.map((booking) => {
    const machine = equipment.find((item) => item.id === booking.machineId)
    return <article className="reservation-card" key={booking.id}><div className="reservation-card-heading"><div><span className="eyebrow">{machine.type}</span><h2>{machine.name}</h2></div><span className={`status-badge ${booking.status === 'cancelled' ? 'danger' : history ? 'warning' : 'success'}`}>{booking.status === 'cancelled' ? 'Anulată' : history ? 'Încheiată' : 'Activă'}</span></div><p>{dateLabel(booking.date)} · <strong>{hourLabel(booking.hour)} – {hourLabel(booking.hour + 1)}</strong></p>{booking.reason && <p className="muted">Motivul anulării: {booking.reason}</p>}{booking.moves?.length > 0 && <p className="muted">Mutată de {booking.moves.length} {booking.moves.length === 1 ? 'oară' : 'ori'}. Ultimul interval anterior: {dateLabel(booking.moves.at(-1).date)}, {hourLabel(booking.moves.at(-1).hour)}.</p>}{!history && <div className="reservation-actions"><button className="primary-btn" onClick={() => onMove(booking)}>Vreau să mut rezervarea →</button><button className="reject-btn" onClick={() => { setCancelling(booking.id); setReason(''); setError('') }}>Anulează rezervarea</button></div>}{cancelling === booking.id && <form className="cancellation-form" onSubmit={confirmCancellation}><fieldset disabled={busy}><legend>De ce anulezi? <span>(opțional)</span></legend>{cancellationReasons.map((value) => <label key={value}><input type="radio" name="cancellation-reason" value={value} checked={reason === value} onChange={() => setReason(value)} />{value}</label>)}<label><input type="radio" name="cancellation-reason" checked={!reason} onChange={() => setReason('')} />Prefer să nu spun</label></fieldset>{error && <p className="reservation-error" role="alert">{error}</p>}<div className="reservation-actions"><button className="reject-btn" type="submit" disabled={busy}>{busy ? 'Se anulează…' : 'Confirmă anularea'}</button><button className="text-button" type="button" disabled={busy} onClick={() => setCancelling(null)}>Păstrează rezervarea</button></div></form>}</article>
  })}</div></main>
}

function BookingPage({ machine, bookings, moving, onBack, onReserve }) {
  const [today, setToday] = useState(() => new Date())
  useEffect(() => {
    const timer = window.setInterval(() => setToday(new Date()), 30000)
    return () => window.clearInterval(timer)
  }, [])
  const [month, setMonth] = useState(() => {
    const initial = moving ? new Date(`${moving.date}T12:00:00`) : today
    return new Date(initial.getFullYear(), initial.getMonth(), 1)
  })
  const [date, setDate] = useState(moving?.date || dateKey(today))
  const [hour, setHour] = useState(null)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const offset = (new Date(month.getFullYear(), month.getMonth(), 1).getDay() + 6) % 7
  const days = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate()
  const occupied = !moving && bookings.some((booking) => booking.machineId === machine.id)
  const slotTaken = (value) => bookings.some((booking) => booking.id !== moving?.id && booking.machineId === machine.id && booking.date === date && booking.hour === value)

  async function submit(event) {
    event.preventDefault()
    if (hour === null || saving) return
    setError('')
    setSaving(true)
    try { await onReserve(machine, date, hour) }
    catch (failure) { setError(failure.message); setSaving(false) }
  }

  return (
    <main className="dashboard booking-page">
      <button className="back-button" onClick={onBack}>← Înapoi la echipamente</button>
      <span className="eyebrow">REZERVAREA TA, ÎN CÂȚIVA PAȘI</span><h1>{moving ? 'Mută rezervarea.' : 'Alege momentul potrivit.'}</h1><p className="muted">{machine.name} · {machine.type} · 60 de minute doar pentru tine.</p>
      <form onSubmit={submit} className="booking-layout">
        <div className="booking-options">
          <section className="calendar-panel" aria-labelledby="calendar-title">
            <h2 id="calendar-title"><span className="step-number">1</span> Alege data</h2>
            <div className="calendar-navigation"><button type="button" aria-label="Luna precedentă" disabled={month.getFullYear() === today.getFullYear() && month.getMonth() === today.getMonth()} onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() - 1, 1))}>←</button><h3 aria-live="polite">{month.toLocaleDateString('ro-RO', { month: 'long', year: 'numeric' })}</h3><button type="button" aria-label="Luna următoare" onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() + 1, 1))}>→</button></div>
            <div className="calendar-grid">
              {['Lu', 'Ma', 'Mi', 'Jo', 'Vi', 'Sâ', 'Du'].map((day) => <span className="weekday" key={day}>{day}</span>)}
              {Array.from({ length: offset }, (_, index) => <span key={`empty-${index}`} />)}
              {Array.from({ length: days }, (_, index) => {
                const value = dateKey(new Date(month.getFullYear(), month.getMonth(), index + 1))
                return <button type="button" key={value} className={`calendar-day ${value === dateKey(today) ? 'is-today' : ''}`} disabled={value < dateKey(today)} aria-label={dateLabel(value)} aria-pressed={date === value} onClick={() => { setDate(value); setHour(null); setError('') }}>{index + 1}</button>
              })}
            </div>
            <p className="calendar-caption"><span /> Astăzi <span className="selected-dot" /> Ziua selectată</p>
          </section>
          <section className="time-panel" aria-labelledby="time-title">
            <h2 id="time-title"><span className="step-number">2</span> Alege ora</h2><p className="muted">{dateLabel(date)} · intervale de o oră</p>
            <div className="time-grid">{Array.from({ length: 24 }, (_, value) => <button type="button" key={value} disabled={occupied || slotTaken(value) || (moving?.date === date && moving?.hour === value) || new Date(`${date}T${hourLabel(value)}`).getTime() <= today.getTime()} aria-pressed={hour === value} onClick={() => { setHour(value); setError('') }}><strong>{hourLabel(value)}</strong><span>– {hourLabel(value + 1)}</span></button>)}</div>
          </section>
        </div>
        <aside className="booking-summary"><span className="eyebrow">TOTUL PREGĂTIT?</span><h2>Rezervarea ta</h2><div className="summary-machine">{machine.type === 'Spălare' ? '🧺' : '☀'}<h3>{machine.name}</h3><p>{machine.type}</p></div><dl><div><dt>Data</dt><dd>{dateLabel(date)}</dd></div><div><dt>Interval</dt><dd>{hour === null ? 'Alege o oră' : `${hourLabel(hour)} – ${hourLabel(hour + 1)}`}</dd></div><div><dt>Durata</dt><dd>60 minute</dd></div></dl><p className="summary-note">{moving ? `Rezervarea actuală: ${dateLabel(moving.date)}, ${hourLabel(moving.hour)}. Intervalul actual rămâne rezervat până confirmi mutarea. Orele ocupate sunt dezactivate.` : 'După confirmare, echipamentul va apărea ocupat până la încheierea intervalului rezervat.'}</p>{occupied && <p className="reservation-error" role="alert">Echipamentul a fost rezervat între timp. Alege alt echipament.</p>}{error && <p className="reservation-error" role="alert">{error}</p>}<button className="primary-btn" type="submit" disabled={hour === null || occupied || saving}>{saving ? 'Se salvează…' : moving ? 'Confirmă mutarea →' : 'Confirmă rezervarea →'}</button></aside>
      </form>
    </main>
  )
}
