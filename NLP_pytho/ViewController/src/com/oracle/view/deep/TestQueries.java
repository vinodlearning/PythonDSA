package com.oracle.view.deep;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

public class TestQueries {
    
    public TestQueries() {
        super();
    }
    
    public static List<String> GETALL_QUERIES() {
        List<String> combined = new ArrayList<>();
        combined.addAll(CONTRACT_CREATION_QUERIES);
        combined.addAll(CONTRACT_INFORMATION_QUERIES);
        combined.addAll(PARTS_QUERIES);
        combined.addAll(FAILED_PARTS_QUERIES);
       // combined.addAll(MIXED_AND_COMPLEX_QUERIES);
     //   combined.addAll(EDGE_CASE_QUERIES);
        combined.addAll(NATURAL_LANGUAGE_QUERIES);
        combined.addAll(SPANISH_QUERIES);
       // combined.addAll(BUSINESS_SCENARIO_QUERIES);
        return combined;
    }

    // Contract Creation Queries
    public static final List<String> CONTRACT_CREATION_QUERIES = Arrays.asList(
        // Basic Creation Requests
        "Tell me how to create a contract",
        "How to create contarct?",
        "Steps to create contract",
        "Can you show me how to make a contract?",
        "What's the process for contract creation?",
        "I need guidance on creating a contract",
        "Walk me through contract creation",
        "Explain how to set up a contract",
        "Instructions for making a contract",
        "Need help understanding contract creation",
        
        // Bot Action Requests
        "Create a contract for me",
        "Can you create contract?",
        "Please make a contract",
        "Generate a contract",
        "I need you to create a contract",
        "Set up a contract",
        "Make me a contract",
        "Initiate contract creation",
        "Start a new contract",
        "Could you draft a contract?",
        
        // Formal Requests
        "How do I create a contract?",
        "What are the steps to make a contract?",
        "Can you explain how to create a contract?",
        "Could you provide contract creation instructions?",
        "I would like to know the contract creation process",
        
        // Informal/Casual Requests
        "How too creat a contract?",
        "Steps for makeing a contrakt",
        "Creat a contract pls",
        "Plz make contract",
        "Need 2 create cntract",
        "How 2 make contract?",
        
        // Incomplete/Fragmented
        "How create contract?",
        "Steps contract creation",
        "Make contract",
        "Contract how make?",
        "Creation steps contract",
        "For me contract create",
        
        // Special Characters and Formatting
        "HOW TO CREATE contract?",
        "create...CONTRACT!",
        "contract (how to)?",
        "contractcreation",
        "contract     make",
        "contract; creation",
        "contract@create",
        "#createcontract",
        "contract*creation*help",
        
        // Extremely Short/Long
        "ctrct",
        "mk",
        "how",
        "Could you possibly be so kind as to tell me the exact step-by-step process for creating a new contractual agreement in this system?",
        "I would really appreciate if you could immediately generate for me a complete contract document with all standard terms and conditions included"
    );

    // Contract Information Queries
    public static final List<String> CONTRACT_INFORMATION_QUERIES = Arrays.asList(
        // Basic Contract Info with Typos
        "What is the effective date for contarct 123456?",
        "Show me contract detials for 789012",
        "When does contrat 456789 expire?",
        "What's the experation date for 234567?",
        "Get contarct informaton for 345678",
        "Whats the efective date for 567890?",
        "Show contarct 678901 details",
        "What is efective date of 789012?",
        "Get contract info for 890123",
        "Show me contarct 123456",
        
        // Contract Numbers Only
        "effective date for 123456",
        "show 789012 details",
        "when does 456789 end",
        "234567 expiration",
        "345678 info",
        "567890 effective date",
        "678901 contract details",
        "789012 expiry date",
        "890123 start date",
        "123456 begin date",
        
        // Customer Information
        "whos the customer for contarct 123456?",
        "customer name for 234567",
        "what customer for contrat 345678?",
        "show custmer for 456789",
        "customer detials for 567890",
        "who is custommer for 678901?",
        "get customer info for contarct 789012",
        "customer number for 890123",
        "show custmer number for 123456",
        "what custommer number for 234567?",
        
        // Payment Terms and Conditions
        "payment terms for contarct 123456",
        "what are paymet terms for 234567?",
        "show payment term for 345678",
        "payement terms for 456789",
        "what payment for contrat 567890?",
        "incoterms for contarct 678901",
        "what incoterm for 789012?",
        "show incotems for 890123",
        "contract lenght for 123456",
        "what contract length for 234567?",
        
        // Contract Dates
        "price experation date for 123456",
        "when price expire for 234567?",
        "price expiry for contarct 345678",
        "show price experation for 456789",
        "what price expire date for 567890?",
        "creation date for contarct 678901",
        "when was 789012 created?",
        "create date for 890123",
        "show creation for contarct 123456",
        "when created 234567?",
        
        // Contract Status and Type
        "what type of contarct 123456?",
        "contract typ for 234567",
        "show contarct type for 345678",
        "what kind contract 456789?",
        "type of contrat 567890",
        "status of contarct 678901",
        "what status for 789012?",
        "show contarct status for 890123",
        "is 123456 active?",
        "contract staus for 234567",
        
        // Comprehensive Details
        "show all details for 123456",
        "get everything for contarct 234567",
        "full info for 345678",
        "complete details contrat 456789",
        "show summary for 567890",
        "overview of contarct 678901",
        "brief for 789012",
        "quick info 890123",
        "details about 123456",
        "information on contarct 234567"
    );

    // Parts Queries
    public static final List<String> PARTS_QUERIES = Arrays.asList(
        // Basic Parts Information
        "What is the lead time for part AE12345?",
        "Show me part detials for BC67890",
        "What lead tim for part DE23456?",
        "Show leadtime for FG78901",
        "What's the leed time for part HI34567?",
        "Get part informaton for JK89012",
        "Show part info for LM45678",
        "What part details for NO90123?",
        "Get part data for PQ56789",
        "Show part summary for RS12345",
        
        // Parts Pricing
        "What's the price for part AE12345?",
        "Show pric for part BC67890",
        "What cost for part DE23456?",
        "Get price info for FG78901",
        "Show pricing for part HI34567",
        "What's the prise for JK89012",
        "Cost of part LM45678",
        "Price details for NO90123",
        "Show part price for PQ56789",
        "What pricing for RS12345?",
        
        // MOQ and UOM
        "What's the MOQ for part AE12345?",
        "Show minimum order for BC67890",
        "What min order qty for DE23456?",
        "MOQ for part FG78901",
        "Minimum order quantity for HI34567",
        "What UOM for part JK89012?",
        "Show unit of measure for LM45678",
        "UOM for NO90123",
        "Unit measure for PQ56789",
        "What unit for part RS12345?",
        
        // Parts Status
        "What's the status of part AE12345?",
        "Show part staus for BC67890",
        "Status for part DE23456",
        "What status FG78901?",
        "Show part status for HI34567",
        "Is part JK89012 active?",
        "Part status for LM45678",
        "What's status of NO90123?",
        "Show status for PQ56789",
        "Status info for RS12345",
        
        // Item Classification
        "What's the item classification for AE12345?",
        "Show item class for BC67890",
        "Classification for part DE23456",
        "What class for FG78901?",
        "Item classification HI34567",
        "Show classification for JK89012",
        "What item class for LM45678?",
        "Classification of NO90123",
        "Item class for PQ56789",
        "Show class for RS12345",
        
        // Invoice Parts
        "Show me invoice parts for 123456",
        "What invoice part for 234567?",
        "List invoce parts for 345678",
        "Show invoice part for 456789",
        "What invoice parts in 567890?",
        "Get invoice part for 678901",
        "Show invoic parts for 789012",
        "List invoice part for 890123",
        "What invoice parts for 123456?",
        "Show all invoice part for 234567",
        
        // Parts in Contracts
        "Show me all parts for contarct 123456",
        "What parts in 234567?",
        "List part for contract 345678",
        "Show parts for contrat 456789",
        "What parts loaded in 567890?",
        "Get parts for contarct 678901",
        "Show all part for 789012",
        "List parts in contract 890123",
        "What parts for 123456?",
        "Show part list for 234567"
    );

    // Failed Parts Queries
    public static final List<String> FAILED_PARTS_QUERIES = Arrays.asList(
        // Basic Failed Parts
        "Show me failed parts for 123456",
        "What failed part for 234567?",
        "List faild parts for 345678",
        "Show failed part for 456789",
        "What parts failed in 567890?",
        "Get failed parts for 678901",
        "Show failing parts for 789012",
        "List failed part for 890123",
        "What failed parts for 123456?",
        "Show all failed part for 234567",
        
        // Error Reasons
        "Why did parts fail for 123456?",
        "Show error reasons for 234567",
        "What errors for failed parts 345678?",
        "Why parts failed in 456789?",
        "Show failure reasons for 567890",
        "What caused parts to fail for 678901?",
        "Error details for failed parts 789012",
        "Why failed parts in 890123?",
        "Show error info for 123456",
        "What errors caused failure for 234567?",
        
        // Parts Error Analysis
        "Show me part errors for 123456",
        "What part error for 234567?",
        "List parts with errors for 345678",
        "Show error parts for 456789",
        "What parts have errors in 567890?",
        "Get parts errors for 678901",
        "Show parts with issues for 789012",
        "List error parts for 890123",
        "What parts errors for 123456?",
        "Show all error parts for 234567",
        
        // Error Columns
        "What columns have errors for 123456?",
        "Show error columns for 234567",
        "Which columns failed for 345678?",
        "What column errors for 456789?",
        "Show failed columns for 567890",
        "Error column details for 678901",
        "What columns with errors for 789012?",
        "Show column failures for 890123",
        "Which columns error for 123456?",
        "Error column info for 234567",
        
        // Data Quality Issues
        "Show me parts with missing data for 123456",
        "What parts missing info for 234567?",
        "List parts with no data for 345678",
        "Show incomplete parts for 456789",
        "What parts missing data in 567890?",
        "Get parts with missing info for 678901",
        "Show parts missing data for 789012",
        "List incomplete parts for 890123",
        "What parts no data for 123456?",
        "Show missing data parts for 234567",
        
        // Loading Errors
        "What errors occurred during loading for 123456?",
        "Show loading errors for 234567",
        "What load errors for 345678?",
        "Show loading issues for 456789",
        "What happened during load for 567890?",
        "Loading error details for 678901",
        "What load problems for 789012?",
        "Show load failures for 890123",
        "Loading issues for 123456",
        "What errors during loading for 234567?",
        
        // Validation Issues
        "List validation issues for 123456",
        "Show validation errors for 234567",
        "What validation problems for 345678?",
        "Show validation issues for 456789",
        "What validation errors in 567890?",
        "Get validation problems for 678901",
        "Show validation failures for 789012",
        "List validation errors for 890123",
        "What validation issues for 123456?",
        "Show validation problems for 234567"
    );

    // Mixed and Complex Queries
    public static final List<String> MIXED_AND_COMPLEX_QUERIES = Arrays.asList(
        // Contract and Parts Combined
        "Show me contarct details and failed parts for 123456",
        "What's the effective date and part errors for 234567?",
        "List all parts and customer info for contrat 345678",
        "Show contract info and failed part for 456789",
        "Get customer name and parts errors for 567890",
        "Show contarct details and part issues for 678901",
        "What effective date and failed parts for 789012?",
        "List contract info and error parts for 890123",
        
        // Natural Language Variations
        "tell me about contract 123456",
        "i need info on 234567",
        "can you show me 345678",
        "what do you know about contarct 456789",
        "give me details for 567890",
        "i want to see 678901",
        "please show 789012",
        "can i get info on 890123",
        "help me with contract 123456",
        "i need help with 234567",
        
        // Casual/Informal
        "whats up with 123456?",
        "hows 234567 looking?",
        "anything wrong with 345678?",
        "is 456789 ok?",
        "problems with 567890?",
        "issues with 678901?",
        "troubles with 789012?",
        "concerns about 890123?",
		        "status on 123456?",
        "update on 234567?"
    );

    // Edge Case and Stress Test Queries
    public static final List<String> EDGE_CASE_QUERIES = Arrays.asList(
        // Empty and Minimal Input
        "",
        " ",
        "?",
        ".",
        "!",
        "contract",
        "part",
        "failed",
        "123456",
        "AE12345",
        
        // Special Characters and Symbols
        "contract@123456#details",
        "part$AE12345%price",
        "failed&parts*789012",
        "contract~123456^info",
        "part+BC67890-status",
        "contract|123456\\details",
        "part[AE12345]price",
        "contract{123456}info",
        "part<BC67890>status",
        "contract(123456)details",
        
        // Numbers and Alphanumeric Combinations
        "123456789012345",
        "ABC123DEF456GHI",
        "contract123part456",
        "part123contract456",
        "123contract456part",
        "ABCDEFGHIJKLMNOP",
        "12345ABCDE67890",
        "contract-123-part-456",
        "part_ABC_contract_123",
        "123.456.789.012",
        
        // Very Long Inputs
        "I need comprehensive detailed information about contract number 123456 including all customer details payment terms incoterms effective dates expiration dates status information and all associated parts with their prices lead times MOQ UOM classifications and any failed parts with error details",
        "Show me everything you know about part AE12345 including price lead time MOQ UOM status item classification and which contracts it belongs to and if there are any errors or issues with this part in any contract",
        "What are all the failed parts for contract 123456 and why did they fail and what columns had errors and what were the specific error messages and when did these errors occur during loading",
        
        // Repeated Words and Patterns
        "contract contract contract 123456",
        "part part part AE12345",
        "failed failed failed parts 123456",
        "show show show details details details",
        "what what what is is is the the the price price price",
        "123456 123456 123456 contract contract contract",
        "AE12345 AE12345 AE12345 part part part",
        
        // Malformed Queries
        "contract 123456 details show me please now",
        "part AE12345 what is the price tell me",
        "failed parts 123456 why did they fail explain",
        "contract details 123456 customer name payment terms",
        "part information AE12345 price lead time MOQ status",
        
        // SQL Injection Attempts
        "contract 123456'; DROP TABLE contracts; --",
        "part AE12345' OR '1'='1",
        "contract 123456 UNION SELECT * FROM users",
        "part AE12345'; DELETE FROM parts; --",
        "contract 123456' AND 1=1",
        
        // HTML/XML Tags
        "<script>alert('contract')</script>123456",
        "<b>contract</b> 123456 details",
        "<contract>123456</contract> information",
        "part <strong>AE12345</strong> price",
        "<?xml version='1.0'?>contract 123456",
        
        // JSON-like Inputs
        "{'contract': '123456', 'action': 'details'}",
        "[contract, 123456, details, show]",
        "{\"part\": \"AE12345\", \"query\": \"price\"}",
        "contract:123456,action:details,format:json",
        
        // URL-like Inputs
        "http://contract/123456/details",
        "https://parts.com/AE12345/price",
        "ftp://contracts/123456/info",
        "contract://123456/details?format=json",
        "part://AE12345/price?currency=USD"
    );

    // Natural Language and Conversational Queries
    public static final List<String> NATURAL_LANGUAGE_QUERIES = Arrays.asList(
        // Polite Requests
        "Could you please show me the details for contract 123456?",
        "Would you mind telling me the price of part AE12345?",
        "I would appreciate if you could provide contract information for 789012",
        "May I please see the failed parts for contract 123456?",
        "Would it be possible to get the customer name for contract 234567?",
        
        // Conversational Style
        "Hey, what's the deal with contract 123456?",
        "So, tell me about part AE12345",
        "Alright, I need to know about contract 789012",
        "Look, can you just show me the failed parts for 123456?",
        "Listen, what's the price for part BC67890?",
        
        // Question Variations
        "Do you know the effective date for contract 123456?",
        "Can you tell me when contract 456789 expires?",
        "Do you have information about part AE12345?",
        "Is there any data on failed parts for contract 123456?",
        "Are you able to show me contract details for 789012?",
        
        // Emotional Context
        "I'm really worried about contract 123456, what's wrong with it?",
        "I'm frustrated with these failed parts for 123456, why did they fail?",
        "I'm excited to see the new contract 789012 details!",
        "I'm confused about part AE12345, can you help?",
        "I'm concerned about the status of contract 456789",
        
        // Time-based Queries
        "What happened to contract 123456 yesterday?",
        "Show me recent failed parts for contract 789012",
        "What's the latest update on contract 456789?",
        "Give me today's status for part AE12345",
        "What changed in contract 123456 this week?",
        
        // Comparative Queries
        "Compare contract 123456 with contract 789012",
        "What's the difference between part AE12345 and BC67890?",
        "Show me contracts similar to 123456",
        "Find parts like AE12345 but cheaper",
        "Which contract is better, 123456 or 789012?",
        
        // Conditional Queries
        "If contract 123456 is active, show me its details",
        "Show part AE12345 only if it's available",
        "Display contract 789012 unless it's expired",
        "Get failed parts for 123456 if any exist",
        "Show customer info for 234567 when possible"
    );

    // Spanish Queries
    public static final List<String> SPANISH_QUERIES = Arrays.asList(
        // Contract Creation in Spanish
        "Cómo crear un contrato?",
        "Dime cómo hacer un contrato",
        "Pasos para crear contrato",
        "Puedes mostrarme cómo hacer un contrato?",
        "Cuál es el proceso para crear contratos?",
        "Necesito ayuda creando un contrato",
        "Explícame cómo crear un contrato",
        "Instrucciones para hacer contratos",
        "Ayuda para crear contrato",
        "Crear un contrato para mí",
        "Puedes crear contrato?",
        "Por favor haz un contrato",
        "Generar un contrato",
        "Necesito que crees un contrato",
        
        // Contract Information in Spanish
        "Mostrar detalles del contrato 123456",
        "Cuál es la fecha efectiva del contrato 123456?",
        "Mostrar información del contrato 789012",
        "Cuándo expira el contrato 456789?",
        "Cuál es la fecha de expiración de 234567?",
        "Obtener información del contrato 345678",
        "Cuál es la fecha efectiva de 567890?",
        "Mostrar detalles del contrato 678901",
        "Cuál es la fecha efectiva de 789012?",
        "Obtener información del contrato 890123",
        "Mostrar contrato 123456",
        
        // Contract dates in Spanish
        "fecha efectiva para 123456",
        "mostrar detalles de 789012",
        "cuándo termina 456789",
        "expiración de 234567",
        "información de 345678",
        "fecha efectiva de 567890",
        "detalles del contrato 678901",
        "fecha de expiración de 789012",
        "fecha de inicio de 890123",
        "fecha de comienzo de 123456",
        
        // Customer Information in Spanish
        "quién es el cliente del contrato 123456?",
        "nombre del cliente para 234567",
        "qué cliente para contrato 345678?",
        "mostrar cliente para 456789",
        "detalles del cliente para 567890",
        "quién es el cliente para 678901?",
        "obtener información del cliente para contrato 789012",
        "número de cliente para 890123",
        "mostrar número de cliente para 123456",
        "qué número de cliente para 234567?",
        
        // Payment Terms in Spanish
        "términos de pago para contrato 123456",
        "cuáles son los términos de pago para 234567?",
        "mostrar términos de pago para 345678",
        "términos de pago para 456789",
        "qué pago para contrato 567890?",
        "incoterms para contrato 678901",
        "qué incoterm para 789012?",
        "mostrar incoterms para 890123",
        "duración del contrato para 123456",
        "cuál es la duración del contrato para 234567?",
        
        // Contract Status in Spanish
        "qué tipo de contrato 123456?",
        "tipo de contrato para 234567",
        "mostrar tipo de contrato para 345678",
        "qué clase de contrato 456789?",
        "tipo de contrato 567890",
        "estado del contrato 678901",
        "qué estado para 789012?",
        "mostrar estado del contrato para 890123",
        "está activo 123456?",
        "estado del contrato para 234567",
        
        // Parts Information in Spanish
        "Cuál es el tiempo de entrega para la parte AE12345?",
        "Mostrar detalles de la parte BC67890",
        "Cuál es el tiempo de entrega para la parte DE23456?",
        "Mostrar tiempo de entrega para FG78901",
        "Cuál es el tiempo de entrega para la parte HI34567?",
        "Obtener información de la parte JK89012",
        "Mostrar información de la parte LM45678",
        "Qué detalles de la parte NO90123?",
        "Obtener datos de la parte PQ56789",
        "Mostrar resumen de la parte RS12345",
        
        // Parts Pricing in Spanish
        "Cuál es el precio de la parte AE12345?",
        "Mostrar precio de la parte BC67890",
        "Cuál es el costo de la parte DE23456?",
        "Obtener información de precio para FG78901",
        "Mostrar precios para la parte HI34567",
        "Cuál es el precio de JK89012",
        "Costo de la parte LM45678",
        "Detalles de precio para NO90123",
        "Mostrar precio de la parte PQ56789",
        "Qué precio para RS12345?",
        
        // Parts MOQ and UOM in Spanish
        "Cuál es el MOQ para la parte AE12345?",
        "Mostrar pedido mínimo para BC67890",
        "Cuál es la cantidad mínima de pedido para DE23456?",
        "MOQ para la parte FG78901",
        "Cantidad mínima de pedido para HI34567",
        "Qué UOM para la parte JK89012?",
        "Mostrar unidad de medida para LM45678",
        "UOM para NO90123",
        "Unidad de medida para PQ56789",
        "Qué unidad para la parte RS12345?",
        
        // Parts Status in Spanish
        "Cuál es el estado de la parte AE12345?",
        "Mostrar estado de la parte BC67890",
        "Estado para la parte DE23456",
        "Qué estado FG78901?",
        "Mostrar estado de la parte HI34567",
        "Está activa la parte JK89012?",
        "Estado de la parte LM45678",
        "Cuál es el estado de NO90123?",
        "Mostrar estado para PQ56789",
        "Información de estado para RS12345",
        
        // Failed Parts in Spanish
        "Mostrar partes fallidas para 123456",
        "Qué parte falló para 234567?",
        "Listar partes fallidas para 345678",
        "Mostrar parte fallida para 456789",
        "Qué partes fallaron en 567890?",
        "Obtener partes fallidas para 678901",
        "Mostrar partes que fallan para 789012",
        "Listar parte fallida para 890123",
        "Qué partes fallidas para 123456?",
        "Mostrar todas las partes fallidas para 234567",
        
        // Error Reasons in Spanish
        "Por qué fallaron las partes para 123456?",
        "Mostrar razones de error para 234567",
        "Qué errores para partes fallidas 345678?",
        "Por qué fallaron las partes en 456789?",
        "Mostrar razones de falla para 567890",
        "Qué causó que las partes fallaran para 678901?",
        "Detalles de error para partes fallidas 789012",
        "Por qué fallaron las partes en 890123?",
        "Mostrar información de error para 123456",
        "Qué errores causaron la falla para 234567?",
        
        // Mixed Spanish Queries
        "Mostrar detalles del contrato y partes fallidas para 123456",
        "Cuál es la fecha efectiva y errores de partes para 234567?",
        "Listar todas las partes e información del cliente para contrato 345678",
        "Mostrar información del contrato y parte fallida para 456789",
        "Obtener nombre del cliente y errores de partes para 567890",
        
        // Natural Language Spanish
        "háblame sobre el contrato 123456",
        "necesito información sobre 234567",
        "puedes mostrarme 345678",
        "qué sabes sobre el contrato 456789",
        "dame detalles para 567890",
        "quiero ver 678901",
        "por favor muestra 789012",
        "puedo obtener información sobre 890123",
        "ayúdame con el contrato 123456",
        "necesito ayuda con 234567",
        
        // Casual Spanish
        "qué pasa con 123456?",
        "cómo se ve 234567?",
        "algo malo con 345678?",
        "está bien 456789?",
        "problemas con 567890?",
        "problemas con 678901?",
        "problemas con 789012?",
		        "preocupaciones sobre 890123?",
        "estado de 123456?",
        "actualización de 234567?",
        
        // Spanish Business Queries
        "Mostrar todos los contratos activos",
        "Cuál es el valor total de contratos expirados?",
        "Mostrar los 10 mejores clientes por valor de contrato",
        "Qué partes tienen la tasa de falla más alta?",
        "Cuáles son las razones de error más comunes?",
        
        // Spanish Polite Requests
        "Podrías por favor mostrarme los detalles del contrato 123456?",
        "Te importaría decirme el precio de la parte AE12345?",
        "Agradecería si pudieras proporcionar información del contrato 789012",
        "Puedo por favor ver las partes fallidas del contrato 123456?",
        "Sería posible obtener el nombre del cliente para el contrato 234567?",
        
        // Spanish Conversational
        "Oye, cuál es el problema con el contrato 123456?",
        "Entonces, háblame sobre la parte AE12345",
        "Bien, necesito saber sobre el contrato 789012",
        "Mira, puedes solo mostrarme las partes fallidas para 123456?",
        "Escucha, cuál es el precio de la parte BC67890?",
        
        // Spanish Time-based
        "Qué pasó con el contrato 123456 ayer?",
        "Mostrar partes fallidas recientes para el contrato 789012",
        "Cuál es la última actualización del contrato 456789?",
        "Dame el estado de hoy para la parte AE12345",
        "Qué cambió en el contrato 123456 esta semana?",
        
        // Spanish with Typos
        "Mostrar detalles del contarto 123456",
        "Cual es el precio de la parte AE12345?",
        "Informacion del cliente para contrato 234567",
        "Partes fallidas para contarto 123456",
        "Estado del contrato 789012",
        "Mostrar terminos de pago para 345678",
        "Que tipo de contrato es 456789?",
        "Cuando expira el contarto 567890?",
        "Mostrar cliente para contrato 678901",
        "Informacion de la parte BC67890"
    );

    // Business Scenario Queries
    public static final List<String> BUSINESS_SCENARIO_QUERIES = Arrays.asList(
        // Executive Queries
        "Give me a summary of all active contracts",
        "What's the total value of expired contracts?",
        "Show me the top 10 customers by contract value",
        "Which parts have the highest failure rate?",
        "What are the most common error reasons?",
        "Show me contract performance metrics",
        "What's the average contract duration?",
        "Which customers have the most contracts?",
        "Show me revenue by contract type",
        "What's the contract renewal rate?",
        
        // Procurement Queries
        "Find all parts with lead time over 30 days",
        "Show contracts expiring in the next 30 days",
        "List parts with MOQ greater than 1000",
        "Which suppliers have the most failed parts?",
        "What's the average price for electronic components?",
        "Show parts with price increases",
        "Find contracts with favorable payment terms",
        "Which parts have the longest lead times?",
        "Show supplier performance metrics",
        "What parts are backordered?",
        
        // Quality Assurance Queries
        "Show all validation errors from last month",
        "Which contracts have the most data quality issues?",
        "List parts with missing classification",
        "What are the top 5 error columns?",
        "Show contracts with incomplete customer information",
        "Find parts with inconsistent pricing",
        "Which contracts have missing documentation?",
        "Show data completeness metrics",
        "What are the most frequent validation failures?",
        "Find contracts with outdated information",
        
        // Operations Queries
        "Which contracts need immediate attention?",
        "Show parts that failed loading today",
        "List contracts with missing payment terms",
        "What parts are pending approval?",
        "Show contracts with status issues",
        "Find contracts requiring renewal",
        "Which parts need price updates?",
        "Show system processing errors",
        "What contracts have compliance issues?",
        "Find parts with inventory problems",
        
        // Customer Service Queries
        "Show all contracts for customer ABC Corp",
        "What's the status of customer inquiry 123456?",
        "List all parts ordered by customer XYZ Ltd",
        "Show contract history for customer DEF Inc",
        "What are the payment terms for customer GHI Co?",
        "Find customer contact information",
        "Show customer satisfaction metrics",
        "What contracts are under dispute?",
        "Find customer billing issues",
        "Show customer order history",
        
        // Financial Queries
        "What's the total contract value this quarter?",
        "Show revenue by customer segment",
        "Find contracts with payment delays",
        "What's the average contract value?",
        "Show profit margins by contract type",
        "Find high-value contracts at risk",
        "What's the collection rate for overdue accounts?",
        "Show pricing trends by part category",
        "Find contracts with cost overruns",
        "What's the return on investment for key contracts?",
        
        // Compliance and Audit Queries
        "Show all contract modifications this quarter",
        "List parts with pricing discrepancies",
        "Which contracts lack proper documentation?",
        "Show all failed validation checks",
        "What contracts have compliance issues?",
        "Find contracts requiring legal review",
        "Show audit trail for contract changes",
        "What contracts violate company policies?",
        "Find unauthorized contract modifications",
        "Show regulatory compliance status",
        
        // Performance and Analytics
        "Show contract processing performance",
        "What's the average time to contract approval?",
        "Find bottlenecks in contract creation",
        "Show user activity metrics",
        "What's the system uptime percentage?",
        "Find slow-performing queries",
        "Show data processing statistics",
        "What's the error rate by process?",
        "Find peak usage times",
        "Show capacity utilization metrics"
    );

    // Performance Test Queries
    public static final List<String> PERFORMANCE_TEST_QUERIES = Arrays.asList(
        // Bulk Contract Queries
        "Show me details for contracts 123456, 234567, 345678, 456789, 567890",
        "Get information for parts AE12345, BC67890, DE23456, FG78901, HI34567",
        "List failed parts for contracts 123456, 234567, 345678, 456789, 567890, 678901, 789012, 890123",
        
        // Complex Multi-entity Queries
        "Show contract 123456 details, customer info, payment terms, all parts, failed parts, and error reasons",
        "Get part AE12345 price, lead time, MOQ, UOM, status, classification, and all contracts it belongs to",
        "Display contract 789012 with customer details, effective date, expiration, status, parts list, and any issues",
        
        // Rapid-fire Similar Queries
        "contract 123456 details",
        "contract 123457 details", 
        "contract 123458 details",
        "contract 123459 details",
        "contract 123460 details",
        "part AE12345 price",
        "part AE12346 price",
        "part AE12347 price",
        "part AE12348 price",
        "part AE12349 price",
        
        // Large Number Variations
        "contract 999999999999999 details",
        "part ZZZZZZZZZZZZZZZ price",
        "failed parts for 888888888888888",
        "contract 111111111111111 customer",
        "part AAAAAAAAAAAAAAAA status"
    );

    // Test Execution Method
    public static void main(String[] args) {
        System.out.println("Total Test Queries: " + GETALL_QUERIES().size());
        System.out.println("Contract Creation: " + CONTRACT_CREATION_QUERIES.size());
        System.out.println("Contract Information: " + CONTRACT_INFORMATION_QUERIES.size());
        System.out.println("Parts Queries: " + PARTS_QUERIES.size());
        System.out.println("Failed Parts: " + FAILED_PARTS_QUERIES.size());
        System.out.println("Mixed Complex: " + MIXED_AND_COMPLEX_QUERIES.size());
        System.out.println("Edge Cases: " + EDGE_CASE_QUERIES.size());
        System.out.println("Natural Language: " + NATURAL_LANGUAGE_QUERIES.size());
        System.out.println("Spanish Queries: " + SPANISH_QUERIES.size());
        System.out.println("Business Scenarios: " + BUSINESS_SCENARIO_QUERIES.size());
        System.out.println("Performance Tests: " + PERFORMANCE_TEST_QUERIES.size());
        
        // Sample some queries for verification
        System.out.println("\n=== Sample Queries ===");
        List<String> allQueries = GETALL_QUERIES();
        for (int i = 0; i < Math.min(10, allQueries.size()); i++) {
            System.out.println((i+1) + ". " + allQueries.get(i));
        }
        
        System.out.println("\n=== Sample Spanish Queries ===");
        for (int i = 0; i < Math.min(5, SPANISH_QUERIES.size()); i++) {
            System.out.println((i+1) + ". " + SPANISH_QUERIES.get(i));
        }
    }
}
