import { useState } from 'react'

const fields = [
  ['lastName', 'Nume'], ['firstName', 'Prenume'], ['faculty', 'Facultate'],
  ['specialization', 'Profil / specializare'], ['academicYear', 'An universitar', '2026–2027'], ['dorm', 'Cămin'],
]

export default function StudentProfile({ profile, onSave, onLogout, onBack }) {
  const [draft, setDraft] = useState(profile)
  const [message, setMessage] = useState('')

  function photoChanged(event) {
    const file = event.target.files?.[0]
    if (!file) return
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 1500000) {
      setMessage('Alege o imagine JPG, PNG sau WebP de maximum 1,5 MB.')
      return
    }
    const reader = new FileReader()
    reader.onload = () => { setDraft((old) => ({ ...old, photo: reader.result })); setMessage('') }
    reader.onerror = () => setMessage('Imaginea nu a putut fi citită.')
    reader.readAsDataURL(file)
  }

  function submit(event) {
    event.preventDefault()
    try {
      onSave(Object.fromEntries(Object.entries(draft).map(([key, value]) => [key, value.trim()])))
      setMessage('Profilul a fost salvat.')
    } catch { setMessage('Profilul nu a putut fi salvat. Încearcă o fotografie mai mică sau verifică stocarea browserului.') }
  }

  return <main className="dashboard profile-page"><button className="back-button" onClick={onBack}>← Înapoi la echipamente</button><span className="eyebrow">CONTUL TĂU</span><h1>Profilul tău</h1><p className="muted">Câteva detalii, pentru o experiență personală.</p><form className="profile-form" onSubmit={submit}><div className="photo-editor">{draft.photo ? <img className="profile-avatar large-avatar" src={draft.photo} alt="Fotografia de profil" /> : <span className="profile-avatar large-avatar" aria-hidden="true">🎓</span>}<label>Fotografie de profil<input type="file" accept="image/jpeg,image/png,image/webp" onChange={photoChanged} /></label>{draft.photo && <button type="button" className="text-button" onClick={() => setDraft({ ...draft, photo: '' })}>Elimină fotografia</button>}</div><div className="profile-fields">{fields.map(([key, label, placeholder]) => <label key={key}>{label}<input required maxLength={100} name={key} placeholder={placeholder || label} value={draft[key] || ''} onChange={(event) => { setDraft({ ...draft, [key]: event.target.value }); setMessage('') }} /></label>)}</div>{message && <p role="status">{message}</p>}<button className="primary-btn" type="submit">Salvează profilul</button></form><div className="profile-logout"><div><h2>Ai terminat pentru moment?</h2><p className="muted">Te poți conecta din nou oricând.</p></div><button className="reject-btn" onClick={onLogout}>Deconectează-te</button></div></main>
}
