import assert from 'node:assert/strict'
import { randomBytes } from 'node:crypto'

const baseUrl = process.env.TEAMFLOW_BASE_URL ?? 'http://127.0.0.1:8081/api/v1'
const adminUsername = process.env.TEAMFLOW_SMOKE_ADMIN_USERNAME
const adminPassword = process.env.TEAMFLOW_SMOKE_ADMIN_PASSWORD
if (!adminUsername || !adminPassword) {
  throw new Error('Set TEAMFLOW_SMOKE_ADMIN_USERNAME and TEAMFLOW_SMOKE_ADMIN_PASSWORD')
}
if (!baseUrl.startsWith('http://127.0.0.1:') && !baseUrl.startsWith('http://localhost:')) {
  throw new Error('The smoke script runs only against a local isolated server')
}

async function request(method, path, { body, token, cookie, csrf = true } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`
  if (cookie) headers.Cookie = cookie
  if (csrf && (path === '/auth/refresh' || path === '/auth/logout')) {
    headers['X-TeamFlow-CSRF'] = '1'
  }
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(15000),
  })
  const setCookie = response.headers.get('set-cookie')
  return {
    status: response.status,
    payload: await response.json(),
    cookie: setCookie?.split(';', 1)[0] ?? null,
    setCookie,
  }
}

function check(label, response, status, code) {
  assert.equal(response.status, status, `${label}: unexpected HTTP status`)
  assert.equal(response.payload.code, code, `${label}: unexpected business code`)
  process.stdout.write(`PASS ${label} ${status}/${code}\n`)
  return response.payload.data
}

const suffix = `${Date.now()}${randomBytes(3).toString('hex')}`
const password = randomBytes(16).toString('hex')
const adminLogin = await request('POST', '/auth/login', {
  body: { username: adminUsername, password: adminPassword },
})
const adminToken = check('admin login', adminLogin, 200, 'OK').accessToken
const adminMe = check('admin identity', await request('GET', '/users/me', {
  token: adminToken,
}), 200, 'OK')
assert.equal(adminMe.systemAdmin, true)
if (process.env.TEAMFLOW_SMOKE_THROTTLE === '1') {
  const unknownLogin = await request('POST', '/auth/login', {
    body: { username: `missing_${suffix}`, password },
  })
  check('unknown account uses generic login error', unknownLogin, 401, 'AUTH.INVALID_CREDENTIALS')
  for (let attempt = 0; attempt < 5; attempt += 1) {
    check(`failed login ${attempt + 1}`, await request('POST', '/auth/login', {
      body: { username: `limited_${suffix}`, password },
    }), 401, 'AUTH.INVALID_CREDENTIALS')
  }
  check('login rate limited', await request('POST', '/auth/login', {
    body: { username: `limited_${suffix}`, password },
  }), 429, 'AUTH.TOO_MANY_ATTEMPTS')
}

const alice = check('create Alice', await request('POST', '/admin/users', {
  token: adminToken,
  body: { username: `p2_alice_${suffix}`, displayName: 'Alice', password },
}), 200, 'OK')
const bob = check('create Bob', await request('POST', '/admin/users', {
  token: adminToken,
  body: { username: `p2_bob_${suffix}`, displayName: 'Bob', password },
}), 200, 'OK')
const aliceLogin = await request('POST', '/auth/login', {
  body: { username: alice.username, password },
})
let aliceToken = check('Alice login', aliceLogin, 200, 'OK').accessToken
assert.match(aliceLogin.setCookie, /HttpOnly/i)
assert.match(aliceLogin.setCookie, /SameSite=Strict/i)
const bobLogin = await request('POST', '/auth/login', {
  body: { username: bob.username, password },
})
let bobToken = check('Bob login', bobLogin, 200, 'OK').accessToken
check('ordinary user denied admin endpoint', await request('GET', '/admin/users', {
  token: bobToken,
}), 403, 'AUTH.FORBIDDEN')
check('admin cannot disable self', await request('PATCH',
  `/admin/users/${adminMe.id}/status`, {
    token: adminToken, body: { status: 'DISABLED' },
  }), 403, 'AUTH.FORBIDDEN')

const project = check('create project', await request('POST', '/projects', {
  token: aliceToken,
  body: { name: `P2 smoke ${suffix}` },
}), 200, 'OK')
assert.equal(project.ownerId, alice.id)
check('project hidden from Bob', await request('GET', `/projects/${project.id}`, {
  token: bobToken,
}), 404, 'PROJECT.NOT_FOUND')
check('add Bob', await request('POST', `/projects/${project.id}/members`, {
  token: aliceToken,
  body: { userId: bob.id },
}), 200, 'OK')
check('Bob sees project', await request('GET', `/projects/${project.id}`, {
  token: bobToken,
}), 200, 'OK')
check('member cannot edit project', await request('PATCH', `/projects/${project.id}`, {
  token: bobToken, body: { name: 'Unauthorized' },
}), 403, 'AUTH.FORBIDDEN')
check('owner edits project', await request('PATCH', `/projects/${project.id}`, {
  token: aliceToken, body: { name: `P2 renamed ${suffix}` },
}), 200, 'OK')
check('owner member protected', await request('DELETE',
  `/projects/${project.id}/members/${alice.id}`, { token: aliceToken }),
409, 'PROJECT.OWNER_PROTECTED')
check('invalid page size', await request('GET', '/projects?pageSize=101', {
  token: aliceToken,
}), 400, 'COMMON.VALIDATION_ERROR')
check('outsider assignment rejected', await request('POST', `/projects/${project.id}/tasks`, {
  token: aliceToken,
  body: { title: 'Invalid assignment', assigneeId: '999999999' },
}), 400, 'TASK.ASSIGNEE_NOT_MEMBER')

const task = check('create assigned task', await request('POST', `/projects/${project.id}/tasks`, {
  token: aliceToken,
  body: { title: 'Smoke task', assigneeId: bob.id, priority: 'HIGH' },
}), 200, 'OK')
const edited = check('assignee edit', await request('PATCH', `/tasks/${task.id}`, {
  token: bobToken,
  body: { version: task.version, description: 'Ready' },
}), 200, 'OK')
check('unknown writable field rejected', await request('PATCH', `/tasks/${task.id}`, {
  token: bobToken,
  body: { version: edited.version, status: 'DONE' },
}), 400, 'COMMON.VALIDATION_ERROR')
check('forbidden field edit', await request('PATCH', `/tasks/${task.id}`, {
  token: bobToken,
  body: { version: edited.version, title: 'Unauthorized rename' },
}), 403, 'AUTH.FORBIDDEN')
check('illegal state jump', await request('POST', `/tasks/${task.id}/transitions`, {
  token: bobToken,
  body: { version: edited.version, toStatus: 'DONE' },
}), 409, 'TASK.INVALID_TRANSITION')
const moved = check('task transition', await request('POST', `/tasks/${task.id}/transitions`, {
  token: bobToken,
  body: { version: edited.version, toStatus: 'IN_PROGRESS' },
}), 200, 'OK')
assert.equal(moved.status, 'IN_PROGRESS')
check('stale version', await request('POST', `/tasks/${task.id}/transitions`, {
  token: bobToken,
  body: { version: task.version, toStatus: 'IN_REVIEW' },
}), 409, 'TASK.VERSION_CONFLICT')
const review = check('assignee submits review', await request('POST',
  `/tasks/${task.id}/transitions`, {
    token: bobToken,
    body: { version: moved.version, toStatus: 'IN_REVIEW' },
  }), 200, 'OK')
check('assignee cannot approve own task', await request('POST',
  `/tasks/${task.id}/transitions`, {
    token: bobToken,
    body: { version: review.version, toStatus: 'DONE' },
  }), 403, 'AUTH.FORBIDDEN')
const done = check('owner accepts task', await request('POST',
  `/tasks/${task.id}/transitions`, {
    token: aliceToken,
    body: { version: review.version, toStatus: 'DONE' },
  }), 200, 'OK')
const reopened = check('owner reopens task', await request('POST',
  `/tasks/${task.id}/transitions`, {
    token: aliceToken,
    body: { version: done.version, toStatus: 'IN_PROGRESS' },
  }), 200, 'OK')
const reviewAgain = check('assignee resubmits review', await request('POST',
  `/tasks/${task.id}/transitions`, {
    token: bobToken,
    body: { version: reopened.version, toStatus: 'IN_REVIEW' },
  }), 200, 'OK')
const rejected = check('owner rejects review', await request('POST',
  `/tasks/${task.id}/transitions`, {
    token: aliceToken,
    body: { version: reviewAgain.version, toStatus: 'IN_PROGRESS' },
  }), 200, 'OK')
const concurrentEdits = await Promise.all([
  request('PATCH', `/tasks/${task.id}`, {
    token: aliceToken, body: { version: rejected.version, title: 'Concurrent A' },
  }),
  request('PATCH', `/tasks/${task.id}`, {
    token: aliceToken, body: { version: rejected.version, title: 'Concurrent B' },
  }),
])
assert.deepEqual(concurrentEdits.map((result) => result.status).sort(), [200, 409])
assert.equal(concurrentEdits.find((result) => result.status === 409)?.payload.code,
  'TASK.VERSION_CONFLICT')
process.stdout.write('PASS concurrent version 200+409\n')
const activityPage = check('task activity', await request('GET', `/tasks/${task.id}/activities`, {
  token: aliceToken,
}), 200, 'OK')
assert.equal(activityPage.total, 9)
check('audit hidden from member', await request('GET', `/projects/${project.id}/audit-logs`, {
  token: bobToken,
}), 403, 'AUTH.FORBIDDEN')
const auditPage = check('audit visible to owner', await request('GET', `/projects/${project.id}/audit-logs`, {
  token: aliceToken,
}), 200, 'OK')
assert.equal(auditPage.total, 12)
check('assigned member protected', await request('DELETE',
  `/projects/${project.id}/members/${bob.id}`, { token: aliceToken }),
409, 'PROJECT.MEMBER_HAS_TASKS')
const latestTask = concurrentEdits.find((result) => result.status === 200).payload.data
check('owner reassigns task', await request('PATCH', `/tasks/${task.id}`, {
  token: aliceToken,
  body: { version: latestTask.version, assigneeId: alice.id },
}), 200, 'OK')
check('remove unassigned member', await request('DELETE',
  `/projects/${project.id}/members/${bob.id}`, { token: aliceToken }),
200, 'OK')
check('removed member loses project access', await request('GET', `/projects/${project.id}`, {
  token: bobToken,
}), 404, 'PROJECT.NOT_FOUND')

const refreshed = await request('POST', '/auth/refresh', { cookie: bobLogin.cookie })
bobToken = check('refresh rotates credential', refreshed, 200, 'OK').accessToken
assert.notEqual(refreshed.cookie, bobLogin.cookie)
check('refresh requires CSRF header', await request('POST', '/auth/refresh', {
  cookie: refreshed.cookie, csrf: false,
}), 403, 'AUTH.FORBIDDEN')
check('old refresh rejected', await request('POST', '/auth/refresh', {
  cookie: bobLogin.cookie,
}), 401, 'AUTH.SESSION_REVOKED')
const refreshedAgain = await request('POST', '/auth/refresh', { cookie: refreshed.cookie })
bobToken = check('valid refresh survives stale replay', refreshedAgain, 200, 'OK').accessToken
check('disable Bob', await request('PATCH', `/admin/users/${bob.id}/status`, {
  token: adminToken,
  body: { status: 'DISABLED' },
}), 200, 'OK')
check('disabled JWT rejected', await request('GET', `/projects/${project.id}`, {
  token: bobToken,
}), 401, 'AUTH.UNAUTHORIZED')
check('disabled refresh rejected', await request('POST', '/auth/refresh', {
  cookie: refreshedAgain.cookie,
}), 401, 'AUTH.SESSION_REVOKED')
check('archive project', await request('POST', `/projects/${project.id}/archive`, {
  token: aliceToken,
}), 200, 'OK')
check('archive forbids task write', await request('POST', `/projects/${project.id}/tasks`, {
  token: aliceToken,
  body: { title: 'Rejected' },
}), 409, 'PROJECT.ARCHIVED')
check('logout Alice', await request('POST', '/auth/logout', {
  cookie: aliceLogin.cookie,
}), 200, 'OK')
check('revoked JWT rejected', await request('GET', '/users/me', {
  token: aliceToken,
}), 401, 'AUTH.UNAUTHORIZED')
