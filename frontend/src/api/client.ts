export type Role = 'BUYER' | 'OPERATOR'
export interface Identity { id: number; username: string; role: Role }
interface CsrfToken { headerName: string; token: string }

export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string) { super(message) }
}

let csrfToken: CsrfToken | null = null
let csrfRequest: Promise<CsrfToken> | null = null
let onUnauthorized: (() => void) | null = null

export function setUnauthorizedHandler(handler: () => void) { onUnauthorized = handler }
export function clearCsrf() { csrfToken = null; csrfRequest = null }

async function send<T>(path: string, init: RequestInit = {}, notifyUnauthorized = true): Promise<T> {
  if (!path.startsWith('/api/') || path.startsWith('//')) throw new Error('Invalid API path')
  let response: Response
  try {
    response = await fetch(path, { ...init, credentials: 'same-origin', cache: 'no-store' })
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', '无法连接服务，请检查网络或稍后重试。')
  }
  if (!response.ok) {
    let code = ''
    try { code = ((await response.json()) as { error?: string }).error ?? '' } catch { /* non-JSON error */ }
    if (response.status === 401 && notifyUnauthorized) { clearCsrf(); onUnauthorized?.() }
    if (response.status === 403) clearCsrf()
    const message = code === 'INVALID_CREDENTIALS' ? '账号或密码不正确。'
      : response.status === 401 ? '登录已失效，请重新登录。'
      : response.status === 403 ? '没有权限或请求已过期，请刷新后重试。'
      : response.status >= 500 ? '服务暂时不可用，请稍后重试。'
      : '请求未完成，请稍后重试。'
    throw new ApiError(response.status, code, message)
  }
  if (response.status === 204) return undefined as T
  try { return await response.json() as T } catch { throw new ApiError(response.status, 'INVALID_RESPONSE', '服务返回了无法识别的数据。') }
}

async function getCsrf(): Promise<CsrfToken> {
  if (csrfToken) return csrfToken
  if (!csrfRequest) {
    csrfRequest = send<CsrfToken>('/api/auth/csrf', {}, false).then(result => {
      if (!result.headerName || !result.token) throw new ApiError(0, 'INVALID_RESPONSE', '无法获取安全令牌，请稍后重试。')
      csrfToken = result
      return result
    }).finally(() => { csrfRequest = null })
  }
  return csrfRequest
}

export async function apiRequest<T>(path: string, init: RequestInit = {}, options: { notifyUnauthorized?: boolean } = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
    const csrf = await getCsrf()
    headers.set(csrf.headerName, csrf.token)
  }
  return send<T>(path, { ...init, headers }, options.notifyUnauthorized !== false)
}

export async function loginRequest(username: string, password: string) {
  const body = new URLSearchParams({ username, password })
  await apiRequest('/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body }, { notifyUnauthorized: false })
  clearCsrf()
}
export async function logoutRequest() {
  await apiRequest('/api/auth/logout', { method: 'POST' })
  clearCsrf()
}
