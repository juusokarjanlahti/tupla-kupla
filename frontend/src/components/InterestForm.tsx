import { useEffect, useState } from 'react'
import { ApiError, fetchActivityTags } from '../api/client'
import './InterestForm.css'

export interface InterestFormSaveResult {
  name?: string
  affinities: Record<string, number>
}

interface InterestFormProps {
  heading: string
  hint: string
  nameField?: { label: string; initialValue: string }
  initialAffinities: Record<string, number>
  onSave: (result: InterestFormSaveResult) => void
}

type LoadState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'ready'; tags: string[] }

const DEFAULT_AFFINITY = 0.5
const BACKEND_UNREACHABLE_MESSAGE =
  "Couldn't reach the backend. Is it running? (docker compose up -d, then ./mvnw spring-boot:run)"

export function InterestForm({
  heading,
  hint,
  nameField,
  initialAffinities,
  onSave,
}: InterestFormProps) {
  const [loadState, setLoadState] = useState<LoadState>({ status: 'loading' })
  const [name, setName] = useState(nameField?.initialValue ?? '')
  const [affinities, setAffinities] =
    useState<Record<string, number>>(initialAffinities)

  useEffect(() => {
    let cancelled = false
    fetchActivityTags()
      .then((tags) => {
        if (cancelled) return
        setLoadState({ status: 'ready', tags })
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
  }, [])

  function handleSliderChange(tag: string, value: number) {
    setAffinities((prev) => ({ ...prev, [tag]: value }))
  }

  function handleSave() {
    onSave({ name: nameField ? name : undefined, affinities })
  }

  return (
    <section aria-labelledby="interest-form-heading">
      <h2 id="interest-form-heading">{heading}</h2>
      <p className="interest-form-hint">{hint}</p>

      {nameField && (
        <div className="field">
          <label htmlFor="interest-form-name">{nameField.label}</label>
          <input
            id="interest-form-name"
            type="text"
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
        </div>
      )}

      {loadState.status === 'loading' && (
        <div aria-busy="true" aria-label="Loading activities">
          {Array.from({ length: 6 }).map((_, index) => (
            <div key={index} className="skeleton-row" />
          ))}
        </div>
      )}

      {loadState.status === 'error' && (
        <div className="interest-form-error" role="alert">
          <p>{loadState.message}</p>
        </div>
      )}

      {loadState.status === 'ready' && (
        <>
          <div className="interest-list">
            {loadState.tags.map((tag) => {
              const value = affinities[tag] ?? DEFAULT_AFFINITY
              return (
                <div className="interest-row" key={tag}>
                  <label htmlFor={`tag-${tag}`}>{tag}</label>
                  <input
                    id={`tag-${tag}`}
                    type="range"
                    min={0}
                    max={1}
                    step={0.05}
                    value={value}
                    onChange={(event) =>
                      handleSliderChange(tag, Number(event.target.value))
                    }
                  />
                  <span className="interest-value">{value.toFixed(2)}</span>
                </div>
              )
            })}
          </div>
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleSave}
          >
            Save
          </button>
        </>
      )}
    </section>
  )
}
