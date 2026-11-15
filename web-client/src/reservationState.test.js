import { test } from 'node:test'
import assert from 'node:assert/strict'
import { changeReservation, isActive, studentId } from './reservationState.js'

const now = new Date('2026-11-15T08:00:00').getTime()
const booking = { id: 'one', ownerId: studentId, machineId: 1, date: '2026-11-16', hour: 10 }

test('cancellation preserves the record and reason while releasing equipment', () => {
  const [cancelled] = changeReservation([booking], 'one', { kind: 'cancel', reason: 'Motive personale' }, now)
  assert.equal(cancelled.reason, 'Motive personale')
  assert.equal(isActive(cancelled, now), false)
  assert.equal(booking.status, undefined)
})

test('moving preserves identity and records the previous interval', () => {
  const [moved] = changeReservation([booking], 'one', { kind: 'move', date: '2026-11-17', hour: 12 }, now)
  assert.equal(moved.id, booking.id)
  assert.equal(moved.machineId, booking.machineId)
  assert.equal(moved.hour, 12)
  assert.equal(moved.moves[0].date, booking.date)
  assert.equal(isActive(moved, now), true)
})

test('a conflict rejects the move without modifying the original reservation', () => {
  const other = { ...booking, id: 'two', ownerId: 'someone-else', hour: 12 }
  assert.throws(() => changeReservation([booking, other], 'one', { kind: 'move', date: booking.date, hour: 12 }, now), /rezervat între timp/)
  assert.equal(booking.hour, 10)
})

test('expired reservations, other owners, past times and unchanged intervals are rejected', () => {
  assert.throws(() => changeReservation([{ ...booking, ownerId: 'other' }], 'one', { kind: 'cancel' }, now))
  assert.throws(() => changeReservation([booking], 'one', { kind: 'cancel' }, new Date('2026-11-17T00:00:00').getTime()))
  assert.throws(() => changeReservation([booking], 'one', { kind: 'move', date: '2026-11-14', hour: 10 }, now))
  assert.throws(() => changeReservation([booking], 'one', { kind: 'move', date: booking.date, hour: booking.hour }, now))
})
