import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = 'https://booksight.onrender.com';

const loginDuration     = new Trend('login_duration',       true);
const booksDuration     = new Trend('books_duration',       true);
const recommendDuration = new Trend('recommend_duration',   true);
const errorRate         = new Rate('error_rate');

export const options = {
    scenarios: {
        warm_up: {
            executor: 'constant-vus',
            vus: 1,
            duration: '10s',
            tags: { scenario: 'warm_up' },
        },
        load_test: {
            executor: 'ramping-vus',
            startVUs: 1,
            stages: [
                { duration: '30s', target: 5  },
                { duration: '30s', target: 10 },
                { duration: '20s', target: 0  },
            ],
            startTime: '12s',
            tags: { scenario: 'load_test' },
        },
    },
    thresholds: {
        login_duration:     ['p(95)<3000'],
        books_duration:     ['p(95)<2000'],
        recommend_duration: ['p(95)<5000'],
        error_rate:         ['rate<0.1'],
    },
};

function getToken() {
    const payload = JSON.stringify({
        email:    'zehra',
        password: '123456',
    });
    const res = http.post(`${BASE_URL}/api/auth/login`, payload, {
        headers: { 'Content-Type': 'application/json' },
    });
    if (res.status === 200) {
        return res.json('token');
    }
    return null;
}

export default function () {
    // ── 1. Login ──────────────────────────────────────────────────────────
    const loginPayload = JSON.stringify({
        email:    'zehra',
        password: '123456',
    });

    const loginRes = http.post(`${BASE_URL}/api/auth/login`, loginPayload, {
        headers: { 'Content-Type': 'application/json' },
        tags:    { endpoint: 'login' },
        timeout: '120s',
    });

    loginDuration.add(loginRes.timings.duration);
    const loginOk = check(loginRes, {
        'login 200': (r) => r.status === 200,
        'token var':  (r) => r.json('token') !== null,
    });
    errorRate.add(!loginOk);

    const token = loginOk ? loginRes.json('token') : null;

    sleep(0.5);

    // ── 2. Kitap listesi (public) ─────────────────────────────────────────
    const booksRes = http.get(`${BASE_URL}/api/books`, {
        tags:    { endpoint: 'books' },
        timeout: '120s',
    });

    booksDuration.add(booksRes.timings.duration);
    const booksOk = check(booksRes, {
        'books 200':   (r) => r.status === 200,
        'content var': (r) => r.json('content') !== null,
    });
    errorRate.add(!booksOk);

    sleep(0.5);

    // ── 3. Öneri (token gerekli) ──────────────────────────────────────────
    if (token) {
        const recRes = http.get(`${BASE_URL}/api/recommendations`, {
            headers: { Authorization: `Bearer ${token}` },
            tags:    { endpoint: 'recommendations' },
            timeout: '120s',
        });

        recommendDuration.add(recRes.timings.duration);
        const recOk = check(recRes, {
            'recommend 200':      (r) => r.status === 200,
            'recommendations var': (r) => r.json('recommendations') !== null,
        });
        errorRate.add(!recOk);
    }

    sleep(1);
}
