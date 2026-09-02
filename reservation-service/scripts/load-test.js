import http from 'k6/http';
import { check } from 'k6';

const SESSION_ID = '11111111-1111-1111-1111-111111111111';

export const options = {
    vus: 20,
    iterations: 20,
};

export default function () {
    const memberId = generateUUID();

    const payload = JSON.stringify({
        sessionId: SESSION_ID,
        memberId: memberId,
        headcount: 1,
    });

    const params = {
        headers: { 'Content-Type': 'application/json' },
    };

    const res = http.post('http://localhost:8083/api/reservations/hold', payload, params);

    check(res, {
        'status is 201': (r) => r.status === 201,
    });

    console.log(`Status: ${res.status}, Body: ${res.body}`);
}

function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        const r = (Math.random() * 16) | 0;
        const v = c === 'x' ? r : (r & 0x3) | 0x8;
        return v.toString(16);
    });
}