NLP TEST AUDIT - ONLY ERROR/UNHANDLED CASES
============================================
Generated: Thu Jul 24 22:33:48 IST 2025
Total Test Cases: 70

test case 26 [ERROR/UNHANDLED]
user input: who is custommer for 678901?
user is asking customer name and number for contarct 678901
corrected input: 
filters entities: [award_number=678901]
display entities: [customer_name,customer_number]== should get from contarcts table exact column names
query: ERROR
action type: UNHANDLED_CASE by contacrtnumber
sql: <p><b>Unknown action type:</b> UNHANDLED_CASE</p>
--------------------------------------------------
test case 32 [ERROR/UNHANDLED]
user input: what are paymet terms for 234567?
corrected input: 
filters entities: [award_number=234567]
display entities: [PAYMENT_TERMS]
query: ERROR
action type: UNHANDLED_CASE same by_contarctnumber
sql: <p><b>Unknown action type:</b> UNHANDLED_CASE</p>
--------------------------------------------------
test case 34 [ERROR/UNHANDLED] same as case 32
user input: payement terms for 456789
corrected input: 
filters entities: [award_number=456789]
display entities: [INCOTERMS]
query: ERROR
action type: UNHANDLED_CASE _by_contarctnumber
sql: <p><b>Unknown action type:</b> UNHANDLED_CASE</p>
--------------------------------------------------
test case 37 [ERROR/UNHANDLED] same as case 34
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
action type: UNHANDLED_CASE by_contarctnumber
sql: <p><b>Unknown action type:</b> UNHANDLED_CASE</p>
--------------------------------------------------
test case 57 [ERROR/UNHANDLED]
user input: what status for 789012?
corrected input: 
filters entities: [award_number=789012]
display entities: [status]
query: ERROR
action type: UNHANDLED_CASE _by_contarctnumber
sql: <p><b>Unknown action type:</b> UNHANDLED_CASE</p>
--------------------------------------------------
test case 59 [ERROR/UNHANDLED]
user input: is 123456 active?
corrected input: 
filters entities: [award_number=1234546]
display entities: [status]
query: ERROR
action type: UNHANDLED_CASE by_contarctnumer
sql: <p><b>Unknown action type:</b> UNHANDLED_CASE</p>
--------------------------------------------------

SUMMARY
=======
Total test cases: 70
Total error/unhandled cases: 7
Error/Unhandled rate: 10.00%
