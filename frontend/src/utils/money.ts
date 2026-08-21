export function formatFen(fen: number): string {
  return `¥${(fen / 100).toFixed(2)}`
}
