import { performance } from 'node:perf_hooks'

const baseUrl = process.env.TEAMFLOW_BASE_URL ?? 'http://127.0.0.1:8081/api/v1'
const username = process.env.TEAMFLOW_BENCH_USERNAME
const password = process.env.TEAMFLOW_BENCH_PASSWORD
const projectId = process.env.TEAMFLOW_BENCH_PROJECT_ID
const count = Number(process.env.TEAMFLOW_BENCH_REQUESTS ?? 100)
const concurrency = Number(process.env.TEAMFLOW_BENCH_CONCURRENCY ?? 5)
if (!/^http:\/\/(127\.0\.0\.1|localhost):\d+\/api\/v1$/.test(baseUrl)
    || !username || !password || !/^[1-9]\d*$/.test(projectId ?? '')
    || !Number.isInteger(count) || count < 1 || count > 1000
    || !Number.isInteger(concurrency) || concurrency < 1 || concurrency > 20) {
  throw new Error('Set local URL, test credentials, project ID, and bounded request settings')
}

const login = await fetch(`${baseUrl}/auth/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username, password }),
  signal: AbortSignal.timeout(15000),
})
const loginBody = await login.json()
if (login.status !== 200 || loginBody.code !== 'OK') {
  throw new Error(`Login failed: ${login.status}/${loginBody.code}`)
}
const token = loginBody.data.accessToken
const cookie = login.headers.get('set-cookie')?.split(';', 1)[0]
const paths = [
  `/projects/${projectId}/stats`,
  `/projects/${projectId}/tasks?page=1&pageSize=20&status=TODO`,
  `/projects/${projectId}/audit-logs?page=1&pageSize=20`,
]
const samples = []
let cursor = 0

async function worker() {
  while (cursor < count) {
    const index = cursor++
    const start = performance.now()
    const response = await fetch(`${baseUrl}${paths[index % paths.length]}`, {
      headers: { Authorization: `Bearer ${token}` },
      signal: AbortSignal.timeout(15000),
    })
    const result = await response.json()
    if (response.status !== 200 || result.code !== 'OK') {
      throw new Error(`Read request failed: ${response.status}/${result.code}`)
    }
    samples.push(performance.now() - start)
  }
}

try {
  await Promise.all(Array.from({ length: concurrency }, () => worker()))
} finally {
  if (cookie) {
    await fetch(`${baseUrl}/auth/logout`, {
      method: 'POST',
      headers: { Cookie: cookie, 'X-TeamFlow-CSRF': '1' },
      signal: AbortSignal.timeout(15000),
    })
  }
}
samples.sort((a, b) => a - b)
const percentile = (p) => Number(samples[Math.ceil(samples.length * p) - 1].toFixed(1))
process.stdout.write(JSON.stringify({
  requests: samples.length,
  concurrency,
  p50Ms: percentile(0.5),
  p95Ms: percentile(0.95),
  maxMs: Number(samples.at(-1).toFixed(1)),
}) + '\n')
