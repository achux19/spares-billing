// "Save as PDF" in the browser proposes the page title as the file name,
// so we set the title just before printing: INV-26-27-00012_2026-11-07_Kumar
const clean = (s: string) => s.replace(/[\\/:*?"<>|]+/g, '-').replace(/\s+/g, '').replace(/-+/g, '-')

/** First 5 letters/digits of a customer's name. Empty for a walk-in or blank name. */
export function shortName(name?: string | null): string {
  if (!name) return ''
  const letters = name.replace(/[^A-Za-z0-9]/g, '')
  if (!letters || /^walkin/i.test(letters)) return ''
  return letters.slice(0, 5)
}

export function pdfName(docNo: string, date: string, name?: string | null): string {
  return [clean(docNo), date, shortName(name)].filter(Boolean).join('_')
}

/** Prints with the given page title, then puts the old title back. */
export function printAs(title: string): void {
  const old = document.title
  const restore = () => {
    document.title = old
    window.removeEventListener('afterprint', restore)
  }
  document.title = title
  window.addEventListener('afterprint', restore)
  window.print()
  setTimeout(restore, 60000) // safety net if the browser never fires afterprint
}
