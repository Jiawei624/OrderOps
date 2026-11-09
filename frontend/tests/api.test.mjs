import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import ts from 'typescript'

const source = await readFile(new URL('../src/api/client.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 } })
const api = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)
const json = (data, status = 200) => new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json' } })

test('login and logout obtain fresh CSRF tokens and preserve session credentials', async () => {
  api.clearCsrf()
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    if (url === '/api/auth/csrf') return json({ headerName: 'X-CSRF-TOKEN', token: `token-${calls.length}` })
    return url.endsWith('logout') ? new Response(null, { status: 204 }) : json({ authenticated: true })
  }
  await api.loginRequest('buyer', 'pass')
  await api.logoutRequest()
  assert.deepEqual(calls.map(call => call.url), ['/api/auth/csrf', '/api/auth/login', '/api/auth/csrf', '/api/auth/logout'])
  assert.equal(calls[1].options.headers.get('X-CSRF-TOKEN'), 'token-1')
  assert.equal(calls[3].options.headers.get('X-CSRF-TOKEN'), 'token-3')
  assert.ok(calls.every(call => call.options.credentials === 'same-origin'))
})

test('403 clears the CSRF token without replaying a write', async () => {
  api.clearCsrf()
  const calls = []
  globalThis.fetch = async (url) => {
    calls.push(url)
    return url === '/api/auth/csrf' ? json({ headerName: 'X-CSRF-TOKEN', token: 'token' }) : json({ error: 'FORBIDDEN' }, 403)
  }
  await assert.rejects(api.apiRequest('/api/example', { method: 'POST' }), error => error.status === 403)
  await assert.rejects(api.apiRequest('/api/example', { method: 'POST' }), error => error.status === 403)
  assert.deepEqual(calls, ['/api/auth/csrf', '/api/example', '/api/auth/csrf', '/api/example'])
})

test('expired session notifies the application, invalid login does not', async () => {
  api.clearCsrf()
  let notifications = 0
  api.setUnauthorizedHandler(() => { notifications++ })
  globalThis.fetch = async url => url === '/api/auth/csrf' ? json({ headerName: 'X-CSRF-TOKEN', token: 'token' }) : json({ error: 'INVALID_CREDENTIALS' }, 401)
  await assert.rejects(api.loginRequest('buyer', 'wrong'), error => error.code === 'INVALID_CREDENTIALS')
  assert.equal(notifications, 0)
  await assert.rejects(api.apiRequest('/api/auth/me'), error => error.status === 401)
  assert.equal(notifications, 1)
})

test('network failures are distinguishable from authentication failures', async () => {
  globalThis.fetch = async () => { throw new TypeError('network down') }
  await assert.rejects(api.apiRequest('/api/auth/me'), error => error.status === 0 && error.code === 'NETWORK_ERROR')
})
