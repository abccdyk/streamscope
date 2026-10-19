import { useEffect, useState } from 'react'
import './App.css'
import StreamList from './StreamList'
import AuthPanel from './AuthPanel'
import type { CurrentUser } from './AuthPanel'
import { useFavoriteGames } from './useFavoriteGames'
import RecommendationList from './RecommendationList'
import { assertResponseOk } from './http'

type Game = {
  id: string
  name: string
  box_art_url: string | null
}

export default function App() {

  const [query, setQuery] = useState('')
  const [games, setGames] = useState<Game[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedGame, setSelectedGame] = useState<Game | null>(null)
  const keyword = query.trim()
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null)

  const {
     favorites,
     ready: favoritesReady,
     busyGameId,
     error: favoriteError,
     toggleFavorite,
   } = useFavoriteGames(currentUser?.id ?? null)
  useEffect(() => {
    const controller = new AbortController()

    setLoading(true)
    setError('')
    setGames([])

    async function loadGames() {
      try {
        const params = new URLSearchParams({ first: '12' })

        let path = '/api/games/top'

        if (keyword) {
          path = '/api/games/search'
          params.set('query', keyword)
        }

        const response = await fetch(`${path}?${params}`, {
          signal: controller.signal,
        })

        await assertResponseOk(response)

        const data: Game[] = await response.json()

        if (!Array.isArray(data)) {
          throw new Error('后端返回的数据格式不正确')
        }

        if (!controller.signal.aborted) {
          setGames(data)
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

    const timer = window.setTimeout(
      () => {
        void loadGames()
      },
      keyword ? 300 : 0,
    )

    return () => {
      window.clearTimeout(timer)
      controller.abort()
    }
  }, [keyword])

  return (
    <main className="app">
      <header className="app-header">
        <p className="eyebrow">TWITCH 内容发现</p>
        <h1>StreamScope</h1>
        <p>发现热门游戏，找到你感兴趣的直播。</p>
      </header>
      <AuthPanel onUserChange={setCurrentUser} />
      <section className="favorites-panel" aria-labelledby="favorites-heading">
        <h2 id="favorites-heading">我的收藏</h2>

        {!currentUser ? (
          <p>登录后可以收藏喜欢的游戏。</p>
        ) : (
          <>
            {favoriteError && (
              <p className="error" role="alert">
                {favoriteError} 如果列表没有更新，请刷新页面。
              </p>
            )}

            {!favoritesReady && !favoriteError && (
              <p role="status">正在加载收藏…</p>
            )}

            {favoritesReady && favorites.length === 0 && (
              <p>还没有收藏，试试下方游戏卡片上的“收藏游戏”。</p>
            )}

            {favoritesReady && (
              <ul className="favorite-list">
                {favorites.map(favorite => (
                  <li key={favorite.gameId}>
                    <span>{favorite.gameName}</span>

                    <div className="favorite-actions">
                      <button
                        type="button"
                        className="action-button"
                        onClick={() => setSelectedGame({
                          id: favorite.gameId,
                          name: favorite.gameName,
                          box_art_url: null,
                        })}
                      >
                        查看直播
                      </button>

                      <button
                        type="button"
                        className="action-button"
                        disabled={busyGameId !== null}
                        onClick={() => void toggleFavorite(favorite.gameId)}
                      >
                        {busyGameId === favorite.gameId
                          ? '处理中…'
                          : '取消收藏'}
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
      </section>
        {currentUser && favoritesReady && (
          <RecommendationList
            key={currentUser.id}
            favoriteKey={favorites
              .map(item => item.gameId)
              .sort()
              .join(',')}
          />
        )}
      <div className="search-panel">
        <label htmlFor="game-search">搜索游戏</label>

        <div className="search-row">
          <input
            id="game-search"
            type="search"
            placeholder="输入游戏名称，例如 Minecraft"
            maxLength={100}
            value={query}
            onChange={event => {
              setQuery(event.target.value)
              setSelectedGame(null)
            }}
          />

          <button
            type="button"
            onClick={() => {
              setQuery('')
              setSelectedGame(null)
            }}
            disabled={query.length === 0}
          >
            返回热门
          </button>
        </div>
      </div>

      <section aria-labelledby="games-heading">
        <h2 id="games-heading">
          {keyword ? `搜索结果：${keyword}` : '热门游戏'}
        </h2>

        {loading && (
          <p role="status">
            {keyword ? '正在搜索游戏…' : '正在加载热门游戏…'}
          </p>
        )}

        {error && (
          <p className="error" role="alert">
            {error}。请确认后端正在运行，然后刷新页面。
          </p>
        )}

        {!loading && !error && games.length === 0 && (
          <p>
            {keyword
              ? '没有找到匹配的游戏，试试其他关键词。'
              : '暂时没有热门游戏。'}
          </p>
        )}

        {!loading && !error && (
          <div className="game-grid">
            {games.map(game => (
              <article className="game-card" key={game.id}>
                {game.box_art_url ? (
                  <img
                    src={game.box_art_url}
                    alt={`${game.name} 封面`}
                    loading="lazy"
                  />
                ) : (
                  <div className="cover-placeholder">暂无封面</div>
                )}
                <div className="game-info">
                  <h3>{game.name}</h3>
                  <p>游戏 ID：{game.id}</p>

                  <button
                    type="button"
                    className="action-button"
                    onClick={() => setSelectedGame(game)}
                  >
                    查看直播
                  </button>
                  <button
                    type="button"
                    className="action-button"
                    disabled={
                      !currentUser ||
                      !favoritesReady ||
                      busyGameId !== null
                    }
                    onClick={() => void toggleFavorite(game.id)}
                  >
                    {!currentUser
                      ? '登录后收藏'
                      : busyGameId === game.id
                        ? '处理中…'
                        : !favoritesReady
                          ? '等待收藏列表'
                          : favorites.some(item => item.gameId === game.id)
                            ? '取消收藏'
                            : '收藏游戏'}
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      {selectedGame && (
        <StreamList
          key={selectedGame.id}
          gameId={selectedGame.id}
          gameName={selectedGame.name}
          onClose={() => setSelectedGame(null)}
        />
      )}
    </main>
  )
}