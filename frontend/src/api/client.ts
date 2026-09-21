import type { ActivityRecommendation, PartnerAnswers } from './types'

export class ApiError extends Error {}

async function parseJsonOrThrow<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new ApiError(`Request failed (${response.status})`)
  }
  return (await response.json()) as T
}

export function fetchActivityTags(): Promise<string[]> {
  return fetch('/api/activities/tags', { credentials: 'include' }).then(
    (response) => parseJsonOrThrow<string[]>(response),
  )
}

export function fetchRecommendations(
  partnerA: PartnerAnswers,
  partnerB: PartnerAnswers,
): Promise<ActivityRecommendation[]> {
  return fetch('/api/recommendations', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ partnerA, partnerB }),
  }).then((response) => parseJsonOrThrow<ActivityRecommendation[]>(response))
}
