import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const CATEGORIES = ['games', 'books', 'collectibles', 'gadgets', "home", null];
const KEYWORDS = ['board', 'space', 'retro', 'limited', 'flash', 'sale', 'top', null];

const REQ_RATE = Number(__ENV.K6_REQ_RATE || 1000); // requests per second
const PRE_ALLOCATED_VUS = Number(__ENV.K6_VUS || 50);
const MAX_VUS = Number(__ENV.K6_MAX_VUS || 100);

export const options = {
  scenarios: {
    browse: {
      executor: 'constant-arrival-rate',
      rate: REQ_RATE,
      timeUnit: '1s',
      duration: __ENV.K6_DURATION || '1m',
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
    },
  },
  cloud: {
    projectID: 5505295,
    name: "Ecommerce"
  }
};

function randomItem(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

export default function () {
  const category = randomItem(CATEGORIES);
  const keyword = randomItem(KEYWORDS);
  const page = Math.floor(Math.random() * 10);
  const size = 10;

  const params = [];
  params.push(`page=${page}`);
  params.push(`size=${size}`);

  if (category) {
    params.push(`category=${category}`);
  }

  if (keyword) {
    params.push(`search=${keyword}`);
  }

  const queryString = params.join('&');

  const res = http.get(`${BASE_URL}/api/v1/products?${queryString}`);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}
