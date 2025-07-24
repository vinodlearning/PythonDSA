NLP TEST AUDIT - ONLY ERROR/UNHANDLED CASES
============================================
Generated: Thu Jul 24 22:25:18 IST 2025
Total Test Cases: 70

test case 26 [ERROR/UNHANDLED]
user input: who is custommer for 678901?
corrected input: 
filters entities: [award_number=688901]
display entities: [customer_name]
query: ERROR
action type: UNHANDLED_CASE=BY_CONTARCTNUMBER
sql: <p><b>Unknown action type:</b> UNHANDLED_CASE</p>
--------------------------------------------------
test case 32 [ERROR/UNHANDLED]
user input: what are paymet terms for 234567?
corrected input: 
filters entities: [award_number=688901]
display entities: [PAYMNET_TREM]
query: ERROR
action type: UNHANDLED_CASE
sql: <p><b>Unknown action type:</b> UNHANDLED_CASE</p>
--------------------------------------------------
test case 34 [ERROR/UNHANDLED]
user input: payement terms for 456789
corrected input: 
filters entities: []
display entities: []
query: ERROR
action type: UNHANDLED_CASE
sql: <p><b>Unknown action type:</b> UNHANDLED_CASE</p>
--------------------------------------------------
test case 37 [ERROR/UNHANDLED]
user input: what incoterm for 789012?
corrected input: 
filters entities: []
display entities: []
query: ERROR
action type: UNHANDLED_CASE
sql: <p><b>Unknown action type:</b> UNHANDLED_CASE</p>
--------------------------------------------------
test case 42 [ERROR/UNHANDLED]
user input: when price expire for 234567?
corrected input: 
filters entities: [award_number=234567]
display entities: [PRICE_EXPIRATION_DATE]
query: ERROR
action type: UNHANDLED_CASE
sql: <p><b>Unknown action type:</b> UNHANDLED_CASE</p>
--------------------------------------------------
test case 57 [ERROR/UNHANDLED]
user input: what status for 789012?
corrected input: 
filters entities: [AWARD_NUMBER=789012]
display entities: [STATUS]
query: ERROR
action type: UNHANDLED_CASEby contract number
sql: <p><b>Unknown action type:</b> UNHANDLED_CASE</p>
--------------------------------------------------
test case 59 [ERROR/UNHANDLED]
user input: is 123456 active?
corrected input: 
filters entities: [award_number=123456]
display entities: [show status column from contarcts table]
query: ERROR
action type: UNHANDLED_CASE_by_contractnumnber
sql: <p><b>Unknown action type:</b> UNHANDLED_CASE</p>
--------------------------------------------------
test case 67 [ERROR/UNHANDLED]
user input: brief for 789012 --simiar to show 1004765 or information about 123456 or infor 1234567 or info 1245635
corrected input: 
filters entities: [award_number=789012]]
display entities: [default columns]
query: ERROR
action type: UNHANDLED_CASE
sql: <p><b>Unknown action type:</b> UNHANDLED_CASE</p>
--------------------------------------------------
test case 69 [ERROR/UNHANDLED]
user input: details about 123456 same as case 67
corrected input: 
filters entities: []
display entities: []
query: ERROR
action type: UNHANDLED_CASE
sql: <p><b>Unknown action type:</b> UNHANDLED_CASE</p>
--------------------------------------------------

SUMMARY
=======
Total test cases: 70
Total error/unhandled cases: 9
Error/Unhandled rate: 12.86%
