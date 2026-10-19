import { useEffect, useState } from 'react'
import { assertResponseOk } from './http'

type LiveStream = {
  id: string
  user_login: string
  user_name: string
  game_id: string
  title: string
  viewer_count: number
  thumbnail_url: string | null
}

type Props = {
  gameId: string
  gameName: string
  onClose: () => void
}

export default function StreamList({
  gameId,
  gameName,
  onClose,
}: Props) {
  const [streams, setStreams] = useState<LiveStream[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [retryCount, setRetryCount] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    setLoading(true)
    setError('')
    setStreams([])

    async function loadStreams() {
      try {
        const params = new URLSearchParams({
          gameId,
          first: '12',
        })

        const response = await fetch(`/api/streams?${params}`, {
          signal: controller.signal,
        })

        await assertResponseOk(response)

        const data: LiveStream[] = await response.json()

        if (!Array.isArray(data)) {
          throw new Error('后端返回的直播数据格式不正确')
        }

        if (!controller.signal.aborted) {
          setStreams(data)
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(err instanceof Error ? err.message : '请求失败')
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      }
    }

    void loadStreams()

    return () => controller.abort()
  }, [gameId, retryCount])

  return (
    <section className="streams-section" aria-labelledby="streams-heading">
      <div className="section-header">
        <h2 id="streams-heading">{gameName} · 在线直播</h2>

        <button type="button" className="action-button" onClick={onClose}>
          关闭列表
        </button>
      </div>

      {loading && <p role="status">正在加载直播…</p>}

      {error && (
        <div className="error" role="alert">
          <p>{error}</p>
          <button
            type="button"
            className="action-button"
            onClick={() => setRetryCount(count => count + 1)}
          >
            重试
          </button>
        </div>
      )}

      {!loading && !error && streams.length === 0 && (
        <p>这个游戏暂时没有在线直播，试试其他游戏。</p>
      )}

      {!loading && !error && (
        <div className="stream-grid">
          {streams.map(stream => (
            <a
              className="stream-card"
              key={stream.id}
              href={`https://www.twitch.tv/${encodeURIComponent(stream.user_login)}`}
              target="_blank"
              rel="noopener noreferrer"
            >
              {stream.thumbnail_url ? (
                <img
                  src={stream.thumbnail_url}
                  alt={`${stream.user_name} 的直播预览`}
                  loading="lazy"
                />
              ) : (
                <div className="stream-placeholder">暂无预览</div>
              )}

              <div className="stream-info">
                <span className="live-badge">直播中</span>
                <h3>{stream.title}</h3>
                <p>{stream.user_name}</p>
                <p>
                  {stream.viewer_count.toLocaleString()} 人观看
                </p>
              </div>
            </a>
          ))}
        </div>
      )}
    </section>
  )
}