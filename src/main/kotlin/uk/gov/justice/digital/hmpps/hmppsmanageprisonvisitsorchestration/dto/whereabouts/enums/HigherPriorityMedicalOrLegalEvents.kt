package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.whereabouts.enums

enum class HigherPriorityMedicalOrLegalEvents(val code: String, val desc: String) {
  ADJUDICATION_HEARING("OIC",	"Adjudication Hearing"),
  CASE_BAIL_APPS("CABA", "Case - Bail Apps"),
  CASE_BENEFITS("CABE", "Case - Benefits"),
  CASE_HOUSING("CAHO", "Case - Housing"),
  CASE_LEGAL_AID("CALA", "Case - Legal Aid"),
  CASE_OTHER("CAOT", "Case - Other"),
  CASE_PROBATION("CAPR", "Case - Probation"),
  CASE_RAPT("RAPT", "Case - RAPT"),
  CASE_SMS("SMS", "Case - SMS"),
  EXAMS("EXAM", "Exams"),
  GOVERNOR("GOVE", "Governor"),
  IMMIGRATION_APPOINTMENT("IMM", "Immigration Appointment"),
  INDUCTION_MEETING("IND", "Induction Meeting"),
  MEDICAL_DENTIST("MEDE", "Medical - Dentist"),
  MEDICAL_DOCTOR("MEDO", "Medical - Doctor"),
  MEDICAL_OPTICIAN("MEOP", "Medical - Optician"),
  MEDICAL_OTHER("MEOT", "Medical - Other"),
  MEDICAL_PSYCHOLOGY_SERVICES("MEPS", "Medical_Psychology Services"),
  MEDICAL_X_RAY("MEXR", "Medical - X-ray"),
  PRE_RELEASE_MEETING("PRM", "Pre-Release Meeting"),
  PSYCHOLOGY_SERVICES_FORENSIC("FOPS", "Psychology Services - Forensic"),
  SOLICITOR_MEETINGS_NON_LEGAL_VISIT("SOLS", "Solicitor Meetings (Not Legal Visit)"),
  VIDEO_LINK_COURT_HEARING("VLB", "Video Link - Court Hearing"),
  VIDEO_LINK_LEGAL_APPOINTMENT("VLLA", "Video Link - Legal Appointment"),
  VIDEO_LINK_PAROLE_HEARING("VLPA", "Video Link - Parole Hearing"),
  VIDEO_LINK_PROBATION_MEETING("VLPM", "Video Link - Probation Meeting"),
}
