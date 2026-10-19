import { useEffect, useRef, useState } from 'react'

export type FavoriteGame = {
  gameId: string
  gameName: string
}

async function checkResponse(response: Response) {
  if (response.ok) return

  if (response.status === 401 || response.status === 403) {
    throw new Error('登录状态可能已过期，请退出后重新登录。')
  }

  const body = await response.json().catch(() => null)

  throw new Error(
    body?.message ||
      body?.detail ||
      `收藏请求失败，状态码：${response.status}`,
  )
}

async function loadFavorites(
  signal: AbortSignal,
): Promise<FavoriteGame[]> {
  const response = await fetch('/api/favorites/games', {
    credentials: 'same-origin',
    cache: 'no-store',
    signal,
  })

  await checkResponse(response)

  const data: FavoriteGame[] = await response.json()

  if (!Array.isArray(data)) {
    throw new Error('收藏列表的数据格式不正确')
  }

  return data
}

export function useFavoriteGames(userId: number | null) {
  const [favorites, setFavorites] = useState<FavoriteGame[]>([])
  const [loadedFor, setLoadedFor] = useState<number | null>(null)
  const [busyGameId, setBusyGameId] = useState<string | null>(null)
  const [error, setError] = useState('')

  const controllerRef = useRef<AbortController | null>(null)
  const busyRef = useRef(false)

  const ready = userId !== null && loadedFor === userId

  useEffect(() => {
    const controller = new AbortController()
    controllerRef.current = controller

    setFavorites([])
    setLoadedFor(null)
    setBusyGameId(null)
    setError('')
    busyRef.current = false

    if (userId !== null) {
      const ownerId = userId

      async function initialize() {
        try {
          const data = await loadFavorites(controller.signal)

          if (!controller.signal.aborted) {
            setFavorites(data)
            setLoadedFor(ownerId)
          }
        } catch (err) {
          if (!controller.signal.aborted) {
            setError(
              err instanceof Error ? err.message : '加载收藏失败',
            )
          }
        }
      }

      void initialize()
    }

    return () => controller.abort()
  }, [userId])

  async function toggleFavorite(gameId: string) {
    const controller = controllerRef.current

    if (
      !ready ||
      busyRef.current ||
      !controller ||
      controller.signal.aborted
    ) {
      return
    }

    const isFavorite = favorites.some(item => item.gameId === gameId)

    busyRef.current = true
    setBusyGameId(gameId)
    setError('')

    try {
      const csrfResponse = await fetch('/api/auth/csrf', {
        credentials: 'same-origin',
        cache: 'no-store',
        signal: controller.signal,
      })

      await checkResponse(csrfResponse)

      const csrf: { headerName: string; token: string } =
        await csrfResponse.json()

      const response = await fetch(
        `/api/favorites/games/${encodeURIComponent(gameId)}`,
        {
          method: isFavorite ? 'DELETE' : 'PUT',
          credentials: 'same-origin',
          headers: {
            [csrf.headerName]: csrf.token,
          },
          signal: controller.signal,
        },
      )

      await checkResponse(response)

      // 修改成功后，重新读取数据库中的实际收藏
      const data = await loadFavorites(controller.signal)

      if (!controller.signal.aborted) {
        setFavorites(data)
      }
    } catch (err) {
      if (!controller.signal.aborted) {
        setError(err instanceof Error ? err.message : '修改收藏失败')
      }
    } finally {
      if (!controller.signal.aborted) {
        busyRef.current = false
        setBusyGameId(null)
      }
    }
  }

  return {
    favorites: ready ? favorites : [],
    ready,
    busyGameId,
    error,
    toggleFavorite,
  }
}