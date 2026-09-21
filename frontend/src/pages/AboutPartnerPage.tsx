import { Link, useNavigate } from 'react-router-dom'
import { InterestForm } from '../components/InterestForm'
import { useCoupleProfile } from '../hooks/useCoupleProfile'

export function AboutPartnerPage() {
  const navigate = useNavigate()
  const { profile, setAffinities, setPartnerName } = useCoupleProfile()

  return (
    <>
      <Link to="/" className="back-link">
        ← Back
      </Link>
      <InterestForm
        heading="Tell me about your partner"
        hint="Rate how interested your partner is in each activity, from 0 (not at all) to 1 (loves it). Leave anything unsure at 0.5."
        nameField={{
          label: "Partner's name",
          initialValue: profile.partner.name,
        }}
        initialAffinities={profile.partner.interestAffinities}
        onSave={({ name, affinities }) => {
          if (name !== undefined) setPartnerName(name)
          setAffinities('partner', affinities)
          navigate('/')
        }}
      />
    </>
  )
}
