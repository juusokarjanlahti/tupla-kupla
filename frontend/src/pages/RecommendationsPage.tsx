import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, fetchRecommendations } from '../api/client'
import type { ActivityRecommendation } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { useCoupleProfile } from '../hooks/useCoupleProfile'
import './Recommendations.css'

type LoadState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'ready'; activities: ActivityRecommendation[] }

const BACKEND_UNREACHABLE_MESSAGE =
  "Couldn't reach the backend. Is it running? (docker compose up -d, then ./mvnw spring-boot:run)"

export function RecommendationsPage() {
  const { user } = useAuth()
  const { profile } = useCoupleProfile()
  const [loadState, setLoadState] = useState<LoadState>({ status: 'loading' })

  useEffect(() => {
    let cancelled = false
    fetchRecommendations(
      {
        name: user?.email ?? 'You',
        interestAffinities: profile.you.interestAffinities,
      },
      {
        name: profile.partner.name || 'Your partner',
        interestAffinities: profile.partner.interestAffinities,
      },
    )
      .then((activities) => {
        if (cancelled) return
        setLoadState({ status: 'ready', activities })
      })
      .catch((error: unknown) => {
        if (cancelled) return
        const message =
          error instanceof ApiError
            ? error.message
            : BACKEND_UNREACHABLE_MESSAGE
        setLoadState({ status: 'error', message })
      })
    return () => {
      cancelled = true
    }
  }, [user, profile])

  return (
    <>
      <Link to="/" className="back-link">
        ← Back
      </Link>
      <section aria-labelledby="recommendations-heading">
        <h2 id="recommendations-heading">Recommended for you two</h2>

        {loadState.status === 'loading' && (
          <div aria-busy="true" aria-label="Crunching the numbers">
            {Array.from({ length: 4 }).map((_, index) => (
              <div key={index} className="skeleton-card" />
            ))}
          </div>
        )}

        {loadState.status === 'error' && (
          <div className="recommendations-error" role="alert">
            <p>{loadState.message}</p>
          </div>
        )}

        {loadState.status === 'ready' && loadState.activities.length === 0 && (
          <p className="recommendations-empty" role="status">
            Nothing came back for this catalog.
          </p>
        )}

        {loadState.status === 'ready' && loadState.activities.length > 0 && (
          <ol className="recommendation-list">
            {loadState.activities.map((activity, index) => {
              const confidencePercent = Math.round(activity.confidence * 100)
              return (
                <li className="recommendation-card" key={activity.id}>
                  <div className="recommendation-rank" aria-hidden="true">
                    #{index + 1}
                  </div>
                  <div className="recommendation-body">
                    <h3>{activity.name}</h3>
                    <div
                      className="confidence-bar"
                      role="img"
                      aria-label={`${confidencePercent}% confidence`}
                    >
                      <div
                        className="confidence-bar-fill"
                        style={{ width: `${confidencePercent}%` }}
                      />
                    </div>
                    <span className="confidence-label">
                      {confidencePercent}% confidence
                    </span>
                  </div>
                </li>
              )
            })}
          </ol>
        )}
      </section>
    </>
  )
}
