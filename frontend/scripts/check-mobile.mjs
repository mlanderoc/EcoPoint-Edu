/**
 * Ergonomics check of the field screens on a 360 px phone with a real Chrome
 * (Spec 004, T324; CA-307, R-301). It reports horizontal overflow and controls
 * shorter than 44 px, saves a screenshot per screen and fails if anything is wrong.
 *
 * Usage (with `npm run dev` running, mock data on):
 *   npm run check:mobile
 * Optional: CHECK_BASE_URL (default http://localhost:5173), CHROME_PATH, CHECK_OUT_DIR.
 *
 * @author craulgomez
 */
import { spawn } from 'node:child_process'
import { mkdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

const baseUrl = process.env.CHECK_BASE_URL ?? 'http://localhost:5173'
const outDir = process.env.CHECK_OUT_DIR ?? 'mobile-check'
const paths = process.argv.slice(2).length > 0
  ? process.argv.slice(2)
  : ['campo', 'campo/turno/14', 'campo/turno/14/poblacion', 'campo/turno/14/bolsas/nueva']
const chromePath = process.env.CHROME_PATH ?? 'C:/Program Files/Google/Chrome/Application/chrome.exe'

const port = 9333
mkdirSync(outDir, { recursive: true })

const chrome = spawn(chromePath, [
  '--headless=new', '--disable-gpu', `--remote-debugging-port=${port}`,
  `--user-data-dir=${join(outDir, 'profile')}`, 'about:blank',
], { stdio: 'ignore' })

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function pageTarget() {
  for (let attempt = 0; attempt < 50; attempt += 1) {
    try {
      const targets = await (await fetch(`http://127.0.0.1:${port}/json`)).json()
      const page = targets.find((target) => target.type === 'page')
      if (page) return page.webSocketDebuggerUrl
    } catch { /* Chrome is still starting */ }
    await sleep(200)
  }
  throw new Error('Chrome no respondió')
}

const socket = new WebSocket(await pageTarget())
await new Promise((resolve) => socket.addEventListener('open', resolve, { once: true }))
let nextId = 0
const pending = new Map()
socket.addEventListener('message', (event) => {
  const message = JSON.parse(event.data)
  if (message.id && pending.has(message.id)) {
    pending.get(message.id)(message)
    pending.delete(message.id)
  }
})
const send = (method, params = {}) => new Promise((resolve) => {
  nextId += 1
  pending.set(nextId, resolve)
  socket.send(JSON.stringify({ id: nextId, method, params }))
})
const evaluate = async (expression) =>
  (await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })).result?.result?.value

await send('Emulation.setDeviceMetricsOverride', { width: 360, height: 800, deviceScaleFactor: 1, mobile: true })
await send('Emulation.setTouchEmulationEnabled', { enabled: true })

const report = []
for (const path of paths) {
  await send('Page.navigate', { url: `${baseUrl}/${path}` })
  let ready = false
  for (let attempt = 0; attempt < 60 && !ready; attempt += 1) {
    await sleep(250)
    ready = await evaluate(`!!document.querySelector('h1') && !document.body.innerText.includes('Cargando')`)
  }
  await sleep(500)
  const result = await evaluate(`(() => {
    const width = document.documentElement.clientWidth
    const scrollWidth = document.documentElement.scrollWidth
    const wide = [...document.querySelectorAll('body *')]
      .filter((el) => el.getBoundingClientRect().right > width + 1)
      .slice(0, 5).map((el) => el.tagName + '.' + String(el.className).slice(0, 60))
    const small = [...document.querySelectorAll('button, input, select, a')]
      .filter((el) => el.offsetParent !== null && el.type !== 'checkbox')
      .filter((el) => el.getBoundingClientRect().height < 44)
      .map((el) => (el.innerText || el.getAttribute('aria-label') || el.id || el.tagName).trim().slice(0, 40)
        + ' (' + Math.round(el.getBoundingClientRect().height) + ' px)')
    return { title: document.querySelector('h1')?.innerText, width, scrollWidth, wide, small }
  })()`)
  const height = await evaluate('document.documentElement.scrollHeight')
  await send('Emulation.setDeviceMetricsOverride', { width: 360, height: Math.min(height, 2400), deviceScaleFactor: 1, mobile: true })
  const shot = await send('Page.captureScreenshot', { format: 'png' })
  writeFileSync(join(outDir, path.replace(/\//g, '_') + '.png'), Buffer.from(shot.result.data, 'base64'))
  await send('Emulation.setDeviceMetricsOverride', { width: 360, height: 800, deviceScaleFactor: 1, mobile: true })
  report.push({ path, ...result })
}

console.log(JSON.stringify(report, null, 2))
socket.close()
chrome.kill()
const failures = report.filter((screen) => screen.scrollWidth > screen.width || screen.small.length > 0)
if (failures.length > 0) {
  console.error(`Pantallas con problemas a 360 px: ${failures.map((screen) => screen.path).join(', ')}`)
  process.exit(1)
}
console.log(`OK: ${report.length} pantallas sin desplazamiento horizontal ni controles pequeños. Capturas en ${outDir}/`)
