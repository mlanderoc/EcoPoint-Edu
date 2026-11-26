/**
 * Choice of one item among several (professor's review, point 7): with a
 * single option it is chosen alone; with more, nothing is chosen by default
 * until the user picks one.
 *
 * @author craulgomez
 */

export function chosenOrOnly<T extends { id: number }>(items: T[] | undefined, chosenId: number | null): T | null {
  if (!items || items.length === 0) return null
  return items.find((item) => item.id === chosenId) ?? (items.length === 1 ? items[0] : null)
}
