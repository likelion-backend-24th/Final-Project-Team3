import { apiFetch } from './client'

// conference-service: PR #57/#65 머지로 organizerName/startAt/endAt/location/description/tags까지 실제로 저장된다.
export function listConferences() {
  return apiFetch('/conferences')
}

export function getConference(id) {
  return apiFetch(`/conferences/${id}`)
}

export function createConference({ organizerName, title, capacity, startAt, endAt, location, description, tags }) {
  return apiFetch('/conferences', {
    method: 'POST',
    body: { organizerName, title, capacity, startAt, endAt, location, description, tags },
  })
}
