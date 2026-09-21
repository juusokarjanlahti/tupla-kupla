export interface ActivityRecommendation {
  id: string
  name: string
  confidence: number
  interestComponent: number
  noveltyComponent: number
  interactivityComponent: number
  socialComponent: number
}

export interface PartnerAnswers {
  name: string
  interestAffinities: Record<string, number>
}
