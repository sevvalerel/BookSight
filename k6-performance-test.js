import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = 'https://booksight.onrender.com';

const loginDuration        = new Trend('login_duration',        true);
const booksDuration        = new Trend('books_duration',        true);
const bookDetailDuration   = new Trend('book_detail_duration',  true);
const leaderboardDuration  = new Trend('leaderboard_duration',  true);
const readingStatusDuration= new Trend('reading_status_duration',true);
const recommendDuration    = new Trend('recommend_duration',    true);
const errorRate            = new Rate('error_rate');

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
        login_duration:         ['p(95)<3000'],
        books_duration:         ['p(95)<2000'],
        book_detail_duration:   ['p(95)<2000'],
        leaderboard_duration:   ['p(95)<2000'],
        reading_status_duration:['p(95)<2000'],
        recommend_duration:     ['p(95)<5000'],
        error_rate:             ['rate<0.1'],
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

    // İlk kitabın ID'sini al
    let bookId = null;
    try {
        const content = booksRes.json('content');
        if (content && content.length > 0) bookId = content[0].bookId;
    } catch (_) {}

    sleep(0.5);

    // ── 3. Kitap detayı (public) ──────────────────────────────────────────
    if (bookId) {
        const bookDetailRes = http.get(`${BASE_URL}/api/books/${bookId}`, {
            tags:    { endpoint: 'book_detail' },
            timeout: '120s',
        });

        bookDetailDuration.add(bookDetailRes.timings.duration);
        const bookDetailOk = check(bookDetailRes, {
            'book detail 200': (r) => r.status === 200,
            'book id var':     (r) => r.json('bookId') !== null,
        });
        errorRate.add(!bookDetailOk);
    }

    sleep(0.5);

    // ── 4. Liderlik tablosu (public) ─────────────────────────────────────
    const leaderboardRes = http.get(`${BASE_URL}/api/users/leaderboard`, {
        tags:    { endpoint: 'leaderboard' },
        timeout: '120s',
    });

    leaderboardDuration.add(leaderboardRes.timings.duration);
    const leaderboardOk = check(leaderboardRes, {
        'leaderboard 200': (r) => r.status === 200,
    });
    errorRate.add(!leaderboardOk);

    sleep(0.5);

    if (token) {
        // ── 5. Okuma durumu (token gerekli) ──────────────────────────────
        const readingRes = http.get(`${BASE_URL}/api/reading-status/my`, {
            headers: { Authorization: `Bearer ${token}` },
            tags:    { endpoint: 'reading_status' },
            timeout: '120s',
        });

        readingStatusDuration.add(readingRes.timings.duration);
        const readingOk = check(readingRes, {
            'reading status 200': (r) => r.status === 200,
        });
        errorRate.add(!readingOk);

        sleep(0.5);

        // ── 6. Öneri (token gerekli) ──────────────────────────────────────
        const recRes = http.get(`${BASE_URL}/api/recommendations`, {
            headers: { Authorization: `Bearer ${token}` },
            tags:    { endpoint: 'recommendations' },
            timeout: '120s',
        });

        recommendDuration.add(recRes.timings.duration);
        const recOk = check(recRes, {
            'recommend 200':       (r) => r.status === 200,
            'recommendations var': (r) => r.json('recommendations') !== null,
        });
        errorRate.add(!recOk);
    }

    sleep(1);
}
