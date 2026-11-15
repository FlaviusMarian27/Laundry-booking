import { useState } from 'react'
import './App.css'
import StudentDashboard from './StudentDashboard'
import useLaundryData from './useLaundryData'

const faults = []
const initialProposals = []

const menuItems = [
  { id: 'dashboard', label: 'Dashboard', icon: '📊' },
  { id: 'machines', label: 'Mașini', icon: '🧺' },
  { id: 'bookings', label: 'Rezervări', icon: '📅' },
  { id: 'faults', label: 'Defecțiuni', icon: '🔧' },
  { id: 'proposals', label: 'Propuneri AI', icon: '🤖' },
  { id: 'feedback', label: 'Feedback', icon: '💬' },
]

function Status({ value }) {
  const className =
    value === 'Disponibilă' || value === 'Aprobată'
      ? 'success'
      : value === 'Defectă' || value === 'Respinsă'
        ? 'danger'
        : 'warning'

  return (
    <span className={`status-badge ${className}`}>
      {value}
    </span>
  )
}

// Pagina de alegere a rolului
function RoleSelection({ onSelect }) {
  return <LoginPage onLogin={onSelect} />
}

function LoginPage({ onLogin }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  function handleSubmit(event) {
    event.preventDefault()
    const account = username.trim()

    if ((account === 'student' || account === 'admin') && password === account) {
      onLogin(account)
      return
    }

    setError('Utilizator sau parolă incorectă.')
  }

  return (
    <main className="role-page">
      <div className="role-container login-container">
        <div className="brand-icon" aria-hidden="true">🧺</div>
        <h1>Laundry Booking</h1>
        <p>Rezervă simplu. Spală fără griji.</p>
        <form className="login-form" onSubmit={handleSubmit}>
          <h2>Conectare</h2>
          <p>Introdu datele contului tău.</p>
          <label htmlFor="username">Email sau utilizator</label>
          <input
            id="username"
            name="username"
            type="text"
            autoComplete="username"
            placeholder="student sau admin"
            value={username}
            onChange={(event) => { setUsername(event.target.value); setError('') }}
            required
            aria-invalid={Boolean(error)}
            aria-describedby={error ? 'login-error' : undefined}
          />
          <label htmlFor="password">Parolă</label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete="current-password"
            placeholder="Introdu parola"
            value={password}
            onChange={(event) => { setPassword(event.target.value); setError('') }}
            required
            aria-invalid={Boolean(error)}
            aria-describedby={error ? 'login-error' : undefined}
          />
          {error && <p id="login-error" className="login-error" role="alert">{error}</p>}
          <button className="primary-btn" type="submit">Conectează-te</button>
          <p className="demo-note">Conturi demo: student / student sau admin / admin.</p>
        </form>
      </div>
    </main>
  )
}

function AdminDashboard({ onLogout }) {
  const { machines, bookings } = useLaundryData()
  const [activePage, setActivePage] = useState('dashboard')
  const [proposals, setProposals] = useState(initialProposals)

  const pending = proposals.filter(
    (proposal) => proposal.status === 'În așteptare'
  ).length

  function handleDecision(id, decision) {
    setProposals((previous) =>
      previous.map((proposal) =>
        proposal.id === id
          ? { ...proposal, status: decision }
          : proposal
      )
    )
  }

  return (
    <div className="admin-layout">
      <aside className="sidebar">
        <h2>🧺 Laundry Booking</h2>
        <p>Administration Panel</p>

        <nav className="sidebar-nav" aria-label="Meniu administrator">
          {menuItems.map((item) => (
            <button
              key={item.id}
              className={activePage === item.id ? 'nav-item active' : 'nav-item'}
              onClick={() => setActivePage(item.id)}
            >
              <span>{item.icon}</span>
              {item.label}
            </button>
          ))}
        </nav>

        <button className="sidebar-logout" onClick={onLogout}>
          ← Schimbă rolul
        </button>
      </aside>

      <div className="admin-main">
        <header className="admin-header">
          <div>
            <h1>
              {menuItems.find((item) => item.id === activePage)?.label}
            </h1>
            <p>Panoul de administrare Laundry Booking</p>
          </div>
          <span className="admin-profile">🛡️ Administrator</span>
        </header>

        <main className="admin-content">
          {activePage === 'dashboard' && (
            <>
              <div className="stats-grid">
                <StatCard icon="🧺" label="Mașini de spălat" value={machines.filter((machine) => machine.type === 'Spălare').length} /><StatCard icon="☀️" label="Uscătoare" value={machines.filter((machine) => machine.type === 'Uscare').length} />
                <StatCard icon="📅" label="Rezervări active" value={bookings.filter((booking) => booking.status === 'Activă').length} />
                <StatCard icon="🤖" label="Propuneri AI noi" value={pending} />
              </div>

              <section className="content-card">
                <h2>Prezentare generală</h2>
                <p>Monitorizează dispozitivele, rezervările și problemele raportate.</p>
                <div className="quick-actions">
                  <button onClick={() => setActivePage('machines')}>
                    Vezi mașinile →
                  </button>
                  <button onClick={() => setActivePage('faults')}>
                    Vezi defecțiunile →
                  </button>
                  <button onClick={() => setActivePage('proposals')}>
                    Verifică propunerile AI →
                  </button>
                </div>
              </section>
            </>
          )}

          {activePage === 'machines' && (
            <section className="content-card">
              <h2>Mașini de spălat și uscătoare</h2>
              <div className="table-wrapper">
                <table>
                  <thead>
                    <tr><th>ID</th><th>Nume</th><th>Tip</th><th>Status</th></tr>
                  </thead>
                  <tbody>
                    {machines.map((machine) => (
                      <tr key={machine.id}>
                        <td>#{machine.id}</td>
                        <td>{machine.name}</td>
                        <td>{machine.type}</td>
                        <td><Status value={machine.status} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          )}

          {activePage === 'bookings' && (
            <section className="content-card">
              <h2>Rezervările studenților</h2>
              <div className="table-wrapper">
                <table>
                  <thead>
                    <tr><th>Student</th><th>Mașină</th><th>Interval</th><th>Status</th></tr>
                  </thead>
                  <tbody>
                    {bookings.length === 0 && <tr><td colSpan={4}>Nu există rezervări înregistrate.</td></tr>}
                    {bookings.map((booking) => (
                      <tr key={booking.id}>
                        <td>{booking.student}</td>
                        <td>{booking.machine}</td>
                        <td>{booking.time}</td>
                        <td><Status value={booking.status} />{booking.reason && <p>{booking.reason}</p>}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          )}

          {activePage === 'faults' && (
            <section className="content-card">
              <h2>Defecțiuni raportate</h2>{faults.length === 0 && <p>Nu există defecțiuni raportate.</p>}
              {faults.map((fault) => (
                <div className="list-row" key={fault.id}>
                  <div>
                    <h3>{fault.machine}</h3>
                    <p>{fault.issue}</p>
                  </div>
                  <Status value={fault.status} />
                </div>
              ))}
            </section>
          )}

          {activePage === 'proposals' && (
            <section className="content-card">
              <h2>🤖 Propunerile agentului AI</h2>{proposals.length === 0 && <p>Nu există propuneri AI disponibile.</p>}
              <p>Administratorul trebuie să aprobe sau să respingă fiecare propunere.</p>

              {proposals.map((proposal) => (
                <div className="proposal-card" key={proposal.id}>
                  <div className="proposal-top">
                    <span className="proposal-type">{proposal.type}</span>
                    <Status value={proposal.status} />
                  </div>

                  <h3>{proposal.title}</h3>
                  <p>{proposal.reason}</p>

                  {proposal.status === 'În așteptare' && (
                    <div className="proposal-actions">
                      <button
                        className="approve-btn"
                        onClick={() => handleDecision(proposal.id, 'Aprobată')}
                      >
                        ✓ Aprobă
                      </button>
                      <button
                        className="reject-btn"
                        onClick={() => handleDecision(proposal.id, 'Respinsă')}
                      >
                        ✕ Respinge
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </section>
          )}

          {activePage === 'feedback' && (
            <section className="content-card">
              <h2>Feedback săptămânal</h2>
              <p>Aici va apărea sumarul feedback-ului trimis de studenți.</p>
              <div className="feedback-placeholder">
                💬 Sumarul va fi disponibil după integrarea cu backend-ul.
              </div>
            </section>
          )}
        </main>
      </div>
    </div>
  )
}

function StatCard({ icon, label, value }) {
  return (
    <div className="stat-card">
      <span className="stat-icon">{icon}</span>
      <p>{label}</p>
      <h2>{value}</h2>
    </div>
  )
}

// Componenta principală decide ce interfață afișează
function App() {
  const [role, setRole] = useState(null)

  if (!role) {
    return <RoleSelection onSelect={setRole} />
  }

  if (role === 'student') {
    return <StudentDashboard onLogout={() => setRole(null)} />
  }

  return <AdminDashboard onLogout={() => setRole(null)} />
}

export default App
