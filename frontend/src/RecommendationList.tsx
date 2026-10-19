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

type RecommendationResponse = {
  mode: 'popular' | 'favorites'
  items: {
    stream: LiveStream
    reason: string
  }[]
}

type Props = {
  favoriteKey: string
}

export default function RecommendationList({ favoriteKey }: Props) {
  const [data, setData] = useState<RecommendationResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshCount, setRefreshCount] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    setLoading(true)
    setError('')
    setData(null)

    async function loadRecommendations() {
      try {
        const response = await fetch('/api/recommendations', {
          credentials: 'same-origin',
          cache: 'no-store',
          signal: controller.signal,
        })

        if (response.status === 401) {
          throw new Error('登录状态已过期，请退出后重新登录。')
        }

        await assertResponseOk(response)

        const result: RecommendationResponse = await response.json()

        if (
          !Array.isArray(result.items) ||
          !['popular', 'favorites'].includes(result.mode)
        ) {
          throw new Error('推荐数据格式不正确')
        }

        if (!controller.signal.aborted) {
          setData(result)
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(
            err instanceof Error ? err.message : '加载推荐失败',
          )
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      }
    }

    void loadRecommendations()

    return () => controller.abort()
  }, [favoriteKey, refreshCount])

  return (
    <section
      className="recommendation-section"
      aria-labelledby="recommendation-heading"
    >
      <div className="section-header">
        <h2 id="recommendation-heading">为你推荐</h2>

        <button
          type="button"
          className="action-button"
          disabled={loading}
          onClick={() => setRefreshCount(count => count + 1)}
        >
          {loading ? '加载中…' : '刷新推荐'}
        </button>
      </div>

      {loading && <p role="status">正在寻找适合你的直播…</p>}

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}

      {!loading && !error && data && (
        <>
          <p className="recommendation-description">
            {data.mode === 'favorites'
              ? '根据你最近收藏的最多 5 个游戏推荐，每个游戏最多展示 4 场直播。'
              : '你还没有收藏游戏，先看看当前热门直播。'}
          </p>

          {data.items.length === 0 ? (
            <p>
              {data.mode === 'favorites'
                ? '本次选取的收藏游戏暂时没有在线直播，可以稍后刷新或收藏其他游戏。'
                : '暂时没有可推荐的直播，请稍后刷新。'}
            </p>
          ) : (
            <div className="stream-grid">
              {data.items.map(item => {
                const stream = item.stream

                return (
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
                      <p className="recommendation-reason">
                        {item.reason}
                      </p>
                    </div>
                  </a>
                )
              })}
            </div>
          )}
        </>
      )}
    </section>
  )
}