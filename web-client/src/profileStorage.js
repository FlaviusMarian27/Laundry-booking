export function readProfile() {
  try {
    const saved = JSON.parse(localStorage.getItem('laundry-profile-v1') || '{}')
    return Object.fromEntries(['lastName', 'firstName', 'faculty', 'specialization', 'academicYear', 'dorm', 'photo'].map((key) => [key, typeof saved?.[key] === 'string' ? saved[key] : '']))
  } catch { return {} }
}

