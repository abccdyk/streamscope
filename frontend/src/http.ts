export async function assertResponseOk(response: Response): Promise<void> {
  if (response.ok) return

  const body = await response.json().catch(() => null)
  const message =
    typeof body?.message === 'string'
      ? body.message
      : `请求失败，状态码：${response.status}`

  throw new Error(message)
}
