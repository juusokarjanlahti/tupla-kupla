import { Link, useNavigate } from 'react-router-dom'
import { InterestForm } from '../components/InterestForm'
import { useCoupleProfile } from '../hooks/useCoupleProfile'

export function AboutYourselfPage() {
  const navigate = useNavigate()
  const { profile, setAffinities } = useCoupleProfile()

  return (
    <>
      <Link to="/" className="back-link">
        ← Back
      </Link>
      <InterestForm
        heading="Tell me about yourself"
        hint="Rate how interested you are in each activity, from 0 (not at all) to 1 (love it). Leave anything unsure at 0.5."
        initialAffinities={profile.you.interestAffinities}
        onSave={({ affinities }) => {
          setAffinities('you', affinities)
          navigate('/')
        }}
      />
    </>
  )
}
