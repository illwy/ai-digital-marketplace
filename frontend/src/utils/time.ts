/** Jackson LocalDateTime has no zone; parse as the browser's local clock. */
export function parseApiDateTime(value: string): number {
  if (/Z$|[+-]\d{2}:\d{2}$/.test(value)) {
    return Date.parse(value)
  }
  const match = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})/.exec(value)
  if (!match) {
    return Date.parse(value)
  }
  return new Date(
    Number(match[1]),
    Number(match[2]) - 1,
    Number(match[3]),
    Number(match[4]),
    Number(match[5]),
    Number(match[6]),
  ).getTime()
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return ''
  }
  const ms = parseApiDateTime(value)
  if (Number.isNaN(ms)) {
    return value
  }
  const date = new Date(ms)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function expireCountdown(expireAt: string | null | undefined): string {
  if (!expireAt) {
    return ''
  }
  const end = parseApiDateTime(expireAt)
  if (Number.isNaN(end)) {
    return ''
  }
  const ms = end - Date.now()
  if (ms <= 0) {
    return '已到期'
  }
  const total = Math.floor(ms / 1000)
  const minutes = Math.floor(total / 60)
  const seconds = total % 60
  return `${minutes}分${String(seconds).padStart(2, '0')}秒`
}
