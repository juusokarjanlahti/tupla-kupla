import { useCallback, useState } from 'react'

export type PartnerKey = 'you' | 'partner'

export interface CoupleProfile {
  you: { interestAffinities: Record<string, number> }
  partner: { name: string; interestAffinities: Record<string, number> }
}

const STORAGE_KEY = 'tupla-kupla:couple-profile'

const EMPTY_PROFILE: CoupleProfile = {
  you: { interestAffinities: {} },
  partner: { name: '', interestAffinities: {} },
}

function readStoredProfile(): CoupleProfile {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return EMPTY_PROFILE
    const parsed = JSON.parse(raw) as Partial<CoupleProfile>
    return {
      you: { interestAffinities: parsed.you?.interestAffinities ?? {} },
      partner: {
        name: parsed.partner?.name ?? '',
        interestAffinities: parsed.partner?.interestAffinities ?? {},
      },
    }
  } catch {
    return EMPTY_PROFILE
  }
}

function writeStoredProfile(profile: CoupleProfile) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(profile))
  } catch {
    // localStorage can be unavailable (private browsing, blocked storage). The app still
    // works for the current visit, it just won't survive a refresh.
  }
}

export function useCoupleProfile() {
  const [profile, setProfile] = useState<CoupleProfile>(readStoredProfile)

  const setAffinities = useCallback(
    (who: PartnerKey, interestAffinities: Record<string, number>) => {
      setProfile((prev) => {
        const next: CoupleProfile = {
          ...prev,
          [who]: { ...prev[who], interestAffinities },
        }
        writeStoredProfile(next)
        return next
      })
    },
    [],
  )

  const setPartnerName = useCallback((name: string) => {
    setProfile((prev) => {
      const next: CoupleProfile = {
        ...prev,
        partner: { ...prev.partner, name },
      }
      writeStoredProfile(next)
      return next
    })
  }, [])

  return { profile, setAffinities, setPartnerName }
}
