
import { useState } from 'react'
import './App.css'

// Date fictive, până când avem backend-ul
const machines = [
  { id: 1, name: 'Mașina de spălat 01', status: 'Disponibilă', type: 'Spălare' },
  { id: 2, name: 'Mașina de spălat 02', status: 'Ocupată', type: 'Spălare' },
  { id: 3, name: 'Mașina de spălat 03', status: 'Disponibilă', type: 'Spălare' },
  { id: 4, name: 'Uscător 01', status: 'Defectă', type: 'Uscare' },
]

const bookings = [
  { id: 1, student: 'Ana Popescu', machine: 'Mașina 01', time: '10:00 - 11:00', status: 'Activă' },
  { id: 2, student: 'Andrei Ionescu', machine: 'Mașina 02', time: '12:00 - 13:00', status: 'Confirmată' },
  { id: 3, student: 'Maria Georgescu', machine: 'Mașina 03', time: '15:00 - 16:00', status: 'Activă' },
]

const faults = [
  { id: 1, machine: 'Uscător 01', issue: 'Nu mai încălzește', status: 'Nerezolvată' },
  { id: 2, machine: 'Mașina 02', issue: 'Vibrații puternice', status: 'În analiză' },
]

const initialProposals = [
  {
    id: 1,
    type: 'Mentenanță',
    title: 'Verificare Uscător 01',
    reason: 'Au fost raportate probleme de încălzire.',
    status: 'În așteptare',
  },
  {
    id: 2,
    type: 'Penalizare',
    title: 'Posibil no-show repetat',
    reason: 'Un student a acumulat rezervări neutilizate.',
    status: 'În așteptare',
  },
]

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
  return (
    <div className="role-page">
      <div className="role-container">
        <div className="brand-icon">🧺</div>

        <h1>Laundry Booking</h1>
        <p>Rezervă simplu. Spală fără griji.</p>

        <h2>Bine ai venit!</h2>
        <p>Alege cum dorești să continui.</p>

        <div className="role-cards">
          <button
            className="role-card"
            onClick={() => onSelect('student')}
          >
            <span className="role-icon">🎓</span>
            <h3>Student</h3>
            <span>Rezervă mașini și verifică disponibilitatea.</span>
            <strong>Continuă ca student →</strong>
          </button>

          <button
            className="role-card"
            onClick={() => onSelect('admin')}
          >
            <span className="role-icon">🛡️</span>
            <h3>Administrator</h3>
            <span>Gestionează mașinile, rezervările și propunerile AI.</span>
            <strong>Continuă ca administrator →</strong>
          </button>
        </div>

        <p className="demo-note">
          Mod demonstrativ — fără autentificare reală
        </p>
      </div>
    </div>
  )
}

// Interfața studentului
function StudentDashboard({ onLogout }) {
  return (
    <div className="app">
      <header className="navbar">
        <h2>🧺 Laundry Booking</h2>
        <div className="header-actions">
          <span>🎓 Student</span>
          <button className="logout-btn" onClick={onLogout}>
            Ieșire
          </button>
        </div>
      </header>

      <main className="dashboard">
        <h1>Bine ai venit! 👋</h1>
        <p>Alege o mașină și rezervă un interval disponibil.</p>

        <h2>Mașinile din cămin</h2>

        <div className="machines-grid">
          {machines.map((machine) => (
            <div className="machine-card" key={machine.id}>
              <div className="machine-icon">🧺</div>
              <h3>{machine.name}</h3>
              <p className="muted">{machine.type}</p>
              <Status value={machine.status} />

              <button
                className="primary-btn"
                disabled={machine.status !== 'Disponibilă'}
                onClick={() => alert(`Ai selectat ${machine.name}. Formularul de rezervare va fi adăugat ulterior.`)}
              >
                Rezervă
              </button>
            </div>
          ))}
        </div>
      </main>
    </div>
  )
}

// Interfața administratorului
function AdminDashboard({ onLogout }) {
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
                <StatCard icon="🧺" label="Total mașini" value={machines.length} />
                <StatCard icon="✅" label="Disponibile" value={machines.filter(m => m.status === 'Disponibilă').length} />
                <StatCard icon="📅" label="Rezervări" value={bookings.length} />
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
              <h2>Gestionarea mașinilor</h2>
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
                    {bookings.map((booking) => (
                      <tr key={booking.id}>
                        <td>{booking.student}</td>
                        <td>{booking.machine}</td>
                        <td>{booking.time}</td>
                        <td><Status value={booking.status} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          )}

          {activePage === 'faults' && (
            <section className="content-card">
              <h2>Defecțiuni raportate</h2>
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
              <h2>🤖 Propunerile agentului AI</h2>
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
