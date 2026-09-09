select
  /*%expand*/*
from
  financial_statements
where
  company_id = /* companyId */1
and
  fiscal_year = /* fiscalYear */2025
and
  fiscal_quarter = /* fiscalQuarter */'Q4'
