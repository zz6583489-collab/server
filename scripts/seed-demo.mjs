import { randomBytes } from 'node:crypto'

const baseUrl = process.env.TEAMFLOW_BASE_URL ?? 'http://127.0.0.1:8081/api/v1'
const adminUsername = process.env.TEAMFLOW_DEMO_ADMIN_USERNAME
const adminPassword = process.env.TEAMFLOW_DEMO_ADMIN_PASSWORD
const userPassword = process.env.TEAMFLOW_DEMO_USER_PASSWORD
if (!/^http:\/\/(127\.0\.0\.1|localhost):\d+\/api\/v1$/.test(baseUrl)) {
  throw new Error('Demo seeding is restricted to a local test server')
}
if (!adminUsername || !adminPassword || !userPassword || userPassword.length < 12) {
  throw new Error('Set demo admin credentials and a 12+ character demo user password')
}

async function api(method, path, body, token) {
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
    signal: AbortSignal.timeout(15000),
  })
  const result = await response.json()
  if (response.status !== 200 || result.code !== 'OK') {
    throw new Error(`${method} ${path} failed: ${response.status}/${result.code}`)
  }
  return result.data
}

const suffix = `${Date.now()}${randomBytes(3).toString('hex')}`
const admin = await api('POST', '/auth/login', {
  username: adminUsername,
  password: adminPassword,
})
const owner = await api('POST', '/admin/users', {
  username: `demo_owner_${suffix}`,
  displayName: '演示负责人',
  password: userPassword,
}, admin.accessToken)
const member = await api('POST', '/admin/users', {
  username: `demo_member_${suffix}`,
  displayName: '演示成员',
  password: userPassword,
}, admin.accessToken)

const ownerLogin = await api('POST', '/auth/login', {
  username: owner.username,
  password: userPassword,
})
const token = ownerLogin.accessToken
const project = await api('POST', '/projects', {
  name: `TeamFlow 演示项目 ${suffix}`,
  description: '用于项目权限、状态机、统计与审计演示',
}, token)
await api('POST', `/projects/${project.id}/members`, { userId: member.id }, token)

async function task(title, priority, dueAt, targetStatus) {
  let created = await api('POST', `/projects/${project.id}/tasks`, {
    title,
    assigneeId: member.id,
    priority,
    ...(dueAt ? { dueAt } : {}),
  }, token)
  const transitions = ['IN_PROGRESS', 'IN_REVIEW', 'DONE']
  for (const status of transitions) {
    if (targetStatus === 'TODO') break
    created = await api('POST', `/tasks/${created.id}/transitions`, {
      toStatus: status,
      version: created.version,
    }, token)
    if (status === targetStatus) break
  }
  return created.id
}

await task('逾期需求梳理', 'HIGH', '2020-01-01T00:00:00Z', 'TODO')
await task('接口联调', 'MEDIUM', null, 'IN_PROGRESS')
await task('负责人评审', 'HIGH', null, 'IN_REVIEW')
await task('发布准备', 'LOW', null, 'DONE')
const stats = await api('GET', `/projects/${project.id}/stats`, undefined, token)
if (stats.total !== 4 || stats.todo !== 1 || stats.inProgress !== 1
    || stats.inReview !== 1 || stats.done !== 1 || stats.overdue !== 1) {
  throw new Error('Demo task distribution did not match the seeded workflow')
}

process.stdout.write(JSON.stringify({
  ownerUsername: owner.username,
  memberUsername: member.username,
  projectId: project.id,
  taskCount: 4,
  completionRatePercent: stats.completionRatePercent,
}) + '\n')
