// Add this to the InsightsController file
@GetMapping("/getCitations")
public Object getCitations(
        @RequestParam(value = "country") String country,
        @RequestParam(value = "patentNo") String patentNo,
        @RequestParam(value = "kind", defaultValue = "") String kind) {
    
    return insightsService.getCitations(country, patentNo, kind);
}

// Add this to the InsightsService file


//
// @Service
// public class InsightsService implements CommandLineRunner {
 //   ...
@Value("${lens.api-key}")
private String lensApiKey;
//...


public Object getCitations(String country, String patentNo, String kind) {
    // Note: make sure the apiKey is stored securely
    String apiKey = lensApiKey;
    String url = "https://api.lens.org/patent/search";
    // Crafting the request body
    String request = "{"
                     + "\"query\":{\"terms\":{\"doc_number\":[\"" + patentNo + "\"]}},"
                     + "\"include\":[\"biblio\"]"
                     + "}";
    
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + apiKey);
    // Here an HttpEntity is created using the previously defined request body and headers. This represents the complete request that will be sent to the Lens API
    HttpEntity<String> entity = new HttpEntity<String>(request, headers);
    
    JSONObject result = new JSONObject();
    
    try {
        LOGGER.info("Fetching citations for patent: " + patentNo);
        // Post request made to the Lens API with the req body we created earlier
        ResponseEntity<Object> response = restTemplate.postForEntity(url, entity, Object.class);
        
        LinkedHashMap<String, Object> body = (LinkedHashMap<String, Object>) response.getBody();
        
        List<Map<String, Object>> patentsData = (List<Map<String, Object>>) body.get("data");
        // Checks if there is at least one patent data entry in the patentsData list. If there is, it processes the first patent's data to extract citation info.
        if (patentsData != null && !patentsData.isEmpty()) {
            Map<String, Object> patent = patentsData.get(0);
            
            Map<String, Object> biblio = (Map<String, Object>) patent.get("biblio");
            
            if (biblio != null) {
                // Extract the references_cited (Backward Citations)
                Map<String, Object> referencesCited = (Map<String, Object>) biblio.get("references_cited");
                if (referencesCited != null) {
                    List<Map<String, Object>> citations = (List<Map<String, Object>>) referencesCited.get("citations");
                    result.put("Backward Citations", citations);
                } else {
                    LOGGER.warn("No backward citations found for patent: " + patentNo);
                    result.put("Backward Citations", "No backward citations available.");
                }
                
                // Extract the cited_by (Forward Citations)
                Map<String, Object> citedBy = (Map<String, Object>) biblio.get("cited_by");
                if (citedBy != null) {
                    List<Map<String, Object>> citationsBy = (List<Map<String, Object>>) citedBy.get("patents");
                    result.put("Forward Citations", citationsBy);
                } else {
                    LOGGER.warn("No forward citations found for patent: " + patentNo);
                    result.put("Forward Citations", "No forward citations available.");
                }
            }
        }
    
    } catch (Exception e) {
        LOGGER.error("Error in getCitations for patent: " + patentNo, e);
        result.put("Backward Citations", "Patent citations unavailable.");
        result.put("Forward Citations", "Patent 'cited by' unavailable.");
    }    
    
    LOGGER.info("Citation fetching completed for patent: " + patentNo);
    return result;
}


// Here is the code for fetching citations from EPO if ever necessary

public Object getCitations(String country, String patentNo, String kind) {
    String accessToken = getEpoAccessToken();
    String patentNumber = constructPatentNumber(country, patentNo, kind);
    String apiUrl = "http://ops.epo.org/3.2/rest-services/family/publication/docdb/" + patentNumber + "/biblio";

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    headers.set("User-Agent", "Cypris.ai");
    HttpEntity<?> entity = new HttpEntity<>(headers);

    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl);
    String uriStr = builder.toUriString();
    RestTemplate restTemplate = new RestTemplate();

    try {
        ResponseEntity<String> response = restTemplate.exchange(uriStr, HttpMethod.GET, entity, String.class);

        JSONParser parser = new JSONParser();
        JSONObject jsonResponse = (JSONObject) parser.parse(response.getBody());

        // Process references-cited recursively
        processReferencesCited(jsonResponse);

        return "Citations processed successfully";
    } catch (Exception e) {
        e.printStackTrace();
        return "Error occurred: " + e.toString();
    }
}

private void processReferencesCited(JSONObject jsonObject) {
    if (jsonObject.containsKey("ops:world-patent-data")) {
        JSONObject bibliographicData = (JSONObject) jsonObject.get("ops:world-patent-data");

        if (bibliographicData.containsKey("references-cited")) {
            JSONObject referencesCited = (JSONObject) bibliographicData.get("references-cited");

            if (referencesCited.containsKey("citation")) {
                JSONArray citations = (JSONArray) referencesCited.get("citation");

                for (Object citationObj : citations) {
                    JSONObject citation = (JSONObject) citationObj;

                    // Print or process the citation information as needed
                    System.out.println("Citation: " + citation);

                    // Recursive call to process nested citations
                    processReferencesCited(citation);
                }
            }
        }
    }
}