UPDATE klanke
SET innsendingsytelse = 'SYKDOM_I_FAMILIE'
WHERE innsendingsytelse = 'OMS'
AND status = 'DRAFT';