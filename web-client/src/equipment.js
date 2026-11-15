export const equipment = ['Spălare', 'Uscare'].flatMap((type, group) =>
  Array.from({ length: 5 }, (_, index) => ({
    id: group * 5 + index + 1,
    name: `${group ? 'Uscător' : 'Mașina de spălat'} ${String(index + 1).padStart(2, '0')}`,
    type,
    status: 'Disponibilă',
  })),
)

