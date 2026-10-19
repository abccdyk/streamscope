import { useEffect, useState } from 'react'

export type CurrentUser = {
  id: number
  username: string
}

type CsrfResponse = {
  headerName: string
  token: string
}

async function checkResponse(response: Response) {
  if (response.ok) return

  const body = await response.json().catch(() => null)

  throw new Error(
    body?.message ||
      body?.detail ||
      `请求失败，状态码：${response.status}`,
  )
}

async function getCsrfHeaders(): Promise<Record<string, string>> {
  const response = await fetch('/api/auth/csrf', {
    credentials: 'same-origin',
    cache: 'no-store',
  })

  await checkResponse(response)

  const csrf: CsrfResponse = await response.json()

  return {
    [csrf.headerName]: csrf.token,
  }
}

type AuthPanelProps = {
  onUserChange: (user: CurrentUser | null) => void
}

export default function AuthPanel({ onUserChange }: AuthPanelProps) {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [checking, setChecking] = useState(true)
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    async function loadCurrentUser() {
      try {
        const response = await fetch('/api/auth/me', {
          credentials: 'same-origin',
          cache: 'no-store',
          signal: controller.signal,
        })

        // 未登录是正常状态
        if (response.status === 401) return

        await checkResponse(response)

        const data: CurrentUser = await response.json()

        if (!controller.signal.aborted) {
          setUser(data)
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(
            err instanceof Error ? err.message : '检查登录状态失败',
          )
        }
      } finally {
        if (!controller.signal.aborted) {
          setChecking(false)
        }
      }
    }

    void loadCurrentUser()

    return () => controller.abort()
  }, [])
  useEffect(() => {
    onUserChange(user)
  }, [user, onUserChange])

  async function submit() {
    if (busy) return

    setBusy(true)
    setError('')
    setNotice('')

    try {
      const csrfHeaders = await getCsrfHeaders()

      if (mode === 'register') {
        const response = await fetch('/api/auth/register', {
          method: 'POST',
          credentials: 'same-origin',
          headers: {
            ...csrfHeaders,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ username, password }),
        })

        await checkResponse(response)

        setMode('login')
        setPassword('')
        setNotice('注册成功，请使用刚才的账号登录。')
      } else {
        const response = await fetch('/api/auth/login', {
          method: 'POST',
          credentials: 'same-origin',
          headers: {
            ...csrfHeaders,
            'Content-Type': 'application/x-www-form-urlencoded',
          },
          body: new URLSearchParams({ username, password }),
        })

        await checkResponse(response)

        const meResponse = await fetch('/api/auth/me', {
          credentials: 'same-origin',
          cache: 'no-store',
        })

        await checkResponse(meResponse)

        const currentUser: CurrentUser = await meResponse.json()

        setUser(currentUser)
        setPassword('')
        setNotice('登录成功。')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '操作失败')
    } finally {
      setBusy(false)
    }
  }

  async function logout() {
    if (busy) return

    setBusy(true)
    setError('')
    setNotice('')

    try {
      const csrfHeaders = await getCsrfHeaders()

      const response = await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'same-origin',
        headers: csrfHeaders,
      })

      await checkResponse(response)

      setUser(null)
      setPassword('')
      setNotice('已退出登录。')
    } catch (err) {
      setError(err instanceof Error ? err.message : '退出失败')
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="auth-panel" aria-labelledby="auth-heading">
      <h2 id="auth-heading">我的账号</h2>

      {checking ? (
        <p role="status">正在检查登录状态…</p>
      ) : user ? (
        <div className="account-row">
          <p>
            当前登录：<strong>{user.username}</strong>
          </p>

          <button
            type="button"
            className="action-button"
            disabled={busy}
            onClick={() => void logout()}
          >
            {busy ? '正在退出…' : '退出登录'}
          </button>
        </div>
      ) : (
        <>
          <div className="auth-tabs">
            <button
              type="button"
              className="action-button"
              aria-pressed={mode === 'login'}
              disabled={busy}
              onClick={() => {
                setMode('login')
                setError('')
                setNotice('')
              }}
            >
              登录
            </button>

            <button
              type="button"
              className="action-button"
              aria-pressed={mode === 'register'}
              disabled={busy}
              onClick={() => {
                setMode('register')
                setError('')
                setNotice('')
              }}
            >
              注册
            </button>
          </div>

          <form
            className="auth-form"
            onSubmit={event => {
              event.preventDefault()
              void submit()
            }}
          >
            <label htmlFor="auth-username">用户名</label>
            <input
              id="auth-username"
              autoComplete="username"
              required
              pattern="[A-Za-z0-9_]{3,32}"
              title="3 到 32 位字母、数字或下划线"
              value={username}
              disabled={busy}
              onChange={event => setUsername(event.target.value)}
            />

            <label htmlFor="auth-password">密码</label>
            <input
              id="auth-password"
              type="password"
              autoComplete={
                mode === 'register' ? 'new-password' : 'current-password'
              }
              required
              minLength={mode === 'register' ? 8 : undefined}
              maxLength={72}
              value={password}
              disabled={busy}
              onChange={event => setPassword(event.target.value)}
            />

            <button
              type="submit"
              className="action-button"
              disabled={busy}
            >
              {busy
                ? '处理中…'
                : mode === 'login'
                  ? '登录账号'
                  : '创建账号'}
            </button>
          </form>
        </>
      )}

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}

      {notice && (
        <p className="auth-notice" role="status">
          {notice}
        </p>
      )}
    </section>
  )
}