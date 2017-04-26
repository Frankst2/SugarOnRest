/**
 MIT License

 Copyright (c) 2017 Kola Oyewumi

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package com.sugaronrest.restapicalls.methodcalls;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.sugaronrest.ErrorResponse;
import com.sugaronrest.restapicalls.responses.InsertRelationshipResponse;
import com.sugaronrest.utils.JsonObjectMapper;


public class InsertRelationship {

    /**
     * Insert relationship [SugarCRM REST method - set_relationship].
     *  
     * @param url
     * @param sessionId
     * @param moduleName
     * @param moduleId
     * @param linkFieldName
     * @param relatedIds
     * @param selectFields
     * @return
     */
    public static InsertRelationshipResponse run(String url, String sessionId,
            String moduleName, String moduleId, String linkFieldName,
            List<String> relatedIds, List<String> selectFields) {

        InsertRelationshipResponse insertRelationship = null;
        ErrorResponse errorResponse = null;

        String jsonRequest = new String();
        String jsonResponse = new String();

        ObjectMapper mapper = JsonObjectMapper.getMapper();

        try {
            Map<String, Object> requestData = new LinkedHashMap<String, Object>();
            requestData.put("session", sessionId);
            requestData.put("module_name", moduleName);
            requestData.put("module_id", moduleId);
            requestData.put("link_field_name", linkFieldName);
            requestData.put("related_ids", relatedIds);
            requestData.put("deleted", 0);

            String jsonRequestData = mapper.writeValueAsString(requestData);

            Map<String, Object> request = new LinkedHashMap<String, Object>();
            request.put("method", "set_relationship");
            request.put("input_type", "json");
            request.put("response_type", "json");
            request.put("rest_data", requestData);

            jsonRequest = mapper.writeValueAsString(request);

            request.put("rest_data", jsonRequestData);

            HttpResponse response = Unirest.post(url)
                    .fields(request)
                    .asString();

            if (response == null) {
                insertRelationship = new InsertRelationshipResponse();
                errorResponse = ErrorResponse.format("An error has occurred!", "No data returned.");
                insertRelationship.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                insertRelationship.setError(errorResponse);
            } else {

                jsonResponse = response.getBody().toString();

                if (StringUtils.isNotBlank(jsonResponse)) {
                    // First check if we have an error
                    errorResponse = ErrorResponse.fromJson(jsonResponse);
                    if (errorResponse == null) {
                        insertRelationship = mapper.readValue(jsonResponse,
                                InsertRelationshipResponse.class);
                    }
                }

                if (insertRelationship == null) {
                    insertRelationship = new InsertRelationshipResponse();
                    insertRelationship.setError(errorResponse);

                    insertRelationship.setStatusCode(HttpStatus.SC_OK);
                    if (errorResponse != null) {
                        insertRelationship
                                .setStatusCode(errorResponse.getStatusCode());
                    }
                } else {
                    insertRelationship.setStatusCode(HttpStatus.SC_OK);
                }
            }
        }
        catch (Exception exception) {
            insertRelationship = new InsertRelationshipResponse();
            errorResponse = ErrorResponse.format(exception, exception.getMessage());
            insertRelationship
                    .setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            errorResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            insertRelationship.setError(errorResponse);
        }

        insertRelationship.setJsonRawRequest(jsonRequest);
        insertRelationship.setJsonRawResponse(jsonResponse);

        return insertRelationship;
    }

    /**
     * Formats and return selected fields.
     *
     * @param entity Java object to update.
     * @param selectFields Selected fields.
     * @return Formatted selected fields.
     */
    private static Map<String, Object> EntityToNameValueList(Object entity, List<String> selectFields) {
        if (entity == null) {
            return null;
        }

        ObjectMapper mapper = JsonObjectMapper.getMapper();
        Map<String, Object> tempEntity = mapper.convertValue(entity, Map.class);

        if (tempEntity == null) {
            return null;
        }

        boolean useSelectedFields = (selectFields != null) && (selectFields.size() > 0);
        Map<String, Object> mappedEntity = new HashMap<String, Object>();
        for (Map.Entry<String, Object> mapEntry : tempEntity.entrySet()) {

            String key = mapEntry.getKey();
            if (useSelectedFields) {
                if (!selectFields.contains(key)) {
                    continue;
                }
            }

            if (key.equalsIgnoreCase("id")) {
                continue;
            }

            Map<String, Object> namevalueDic = new HashMap<String, Object>();
            namevalueDic.put("name", key);
            namevalueDic.put("value", mapEntry.getValue());

            mappedEntity.put(key, namevalueDic);
        }

        return mappedEntity;
    }
}
