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

package com.sugaronrest.restapicalls;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sugaronrest.ErrorResponse;
import com.sugaronrest.Options;
import com.sugaronrest.OptionsRelationship;
import com.sugaronrest.SugarRestRequest;
import com.sugaronrest.SugarRestResponse;
import com.sugaronrest.restapicalls.methodcalls.Authentication;
import com.sugaronrest.restapicalls.methodcalls.DeleteEntry;
import com.sugaronrest.restapicalls.methodcalls.GetAvailableModules;
import com.sugaronrest.restapicalls.methodcalls.GetEntry;
import com.sugaronrest.restapicalls.methodcalls.GetEntryList;
import com.sugaronrest.restapicalls.methodcalls.GetLinkedEntry;
import com.sugaronrest.restapicalls.methodcalls.GetLinkedEntryList;
import com.sugaronrest.restapicalls.methodcalls.GetPagedEntryList;
import com.sugaronrest.restapicalls.methodcalls.InsertEntries;
import com.sugaronrest.restapicalls.methodcalls.InsertEntry;
import com.sugaronrest.restapicalls.methodcalls.InsertLinkedEntry;
import com.sugaronrest.restapicalls.methodcalls.InsertRelationship;
import com.sugaronrest.restapicalls.methodcalls.UpdateEntries;
import com.sugaronrest.restapicalls.methodcalls.UpdateEntry;
import com.sugaronrest.restapicalls.requests.LoginRequest;
import com.sugaronrest.restapicalls.responses.DeleteEntryResponse;
import com.sugaronrest.restapicalls.responses.InsertEntriesResponse;
import com.sugaronrest.restapicalls.responses.InsertEntryResponse;
import com.sugaronrest.restapicalls.responses.InsertRelationshipResponse;
import com.sugaronrest.restapicalls.responses.LoginResponse;
import com.sugaronrest.restapicalls.responses.ReadAvailableModulesResponse;
import com.sugaronrest.restapicalls.responses.ReadEntryListResponse;
import com.sugaronrest.restapicalls.responses.ReadEntryResponse;
import com.sugaronrest.restapicalls.responses.ReadLinkedEntryListResponse;
import com.sugaronrest.restapicalls.responses.ReadLinkedEntryResponse;
import com.sugaronrest.restapicalls.responses.UpdateEntriesResponse;
import com.sugaronrest.restapicalls.responses.UpdateEntryResponse;
import com.sugaronrest.utils.JsonObjectMapper;

public class SugarRestClientExt {

    /**
     * Gets entity by id.
     *
     *  @param request The request object.
     *  @param moduleInfo The entity model info.
     *  @return SugarRestResponse object.
     */
    public static SugarRestResponse executeGetById(SugarRestRequest request, ModuleInfo moduleInfo) {
        SugarRestResponse sugarRestResponse = new SugarRestResponse();
        LoginResponse loginResponse = new LoginResponse();
        ObjectMapper mapper = JsonObjectMapper.getMapper();

        try {
            LoginRequest loginRequest = new LoginRequest(request.getUrl(), request.getUsername(), request.getPassword());
            loginResponse = Authentication.login(loginRequest);

            String url = request.getUrl();
            String sessionId = loginResponse.sessionId;
            String moduleName = moduleInfo.name;
            Options options = request.getOptions();
            String identifier = (String)request.getParameter();

            ReadEntryResponse response = GetEntry.run(url, sessionId, moduleName, identifier, options.getSelectFields());
            if (response != null) {
                sugarRestResponse.setJsonRawRequest(response.getJsonRawRequest());
                sugarRestResponse.setJsonRawResponse(response.getJsonRawResponse());
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    Object entity = response.getEntity();
                    String jsonEnitity = mapper.writeValueAsString(entity);
                    sugarRestResponse.setJData(jsonEnitity);
                    sugarRestResponse.setStatusCode(response.getStatusCode());

                    // Serialize json data to concrete Pojo object.
                    Class moduleClass = moduleInfo.type;
                    if (moduleClass != null) {
                        Object pojoObj = mapper.readValue(jsonEnitity,  moduleClass);
                        sugarRestResponse.setData(pojoObj);
                    } else {
                        sugarRestResponse.setData(entity);
                    }
                } else {
                    sugarRestResponse.setError(response.getError());
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                    sugarRestResponse.setJData(StringUtils.EMPTY);
                    sugarRestResponse.setData(new Object());
                }
            }

            return sugarRestResponse;
        } catch (Exception exception) {
            sugarRestResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            sugarRestResponse.setError(ErrorResponse.format(exception, StringUtils.EMPTY));
        } finally {
            Authentication.logout(request.getUrl(), loginResponse.sessionId);
        }

        return sugarRestResponse;
    }

    /**
     * Gets all entities limited by MaxResultCount sets in request options.
     *
     *  @param request The request object.
     *  @param moduleInfo The entity model info.
     *  @return SugarRestResponse object.
     */
    public static SugarRestResponse executeGetAll(SugarRestRequest request, ModuleInfo moduleInfo) {
        SugarRestResponse sugarRestResponse = new SugarRestResponse();
        LoginResponse loginResponse = new LoginResponse();
        ObjectMapper mapper = JsonObjectMapper.getMapper();

        try {
            LoginRequest loginRequest = new LoginRequest(request.getUrl(), request.getUsername(), request.getPassword());
            loginResponse = Authentication.login(loginRequest);

            String url = request.getUrl();
            String sessionId = loginResponse.sessionId;
            String moduleName = moduleInfo.name;
            Options options = request.getOptions();

            String query = ModuleInfoExt.getQuery(moduleInfo, options.getQueryPredicates(), options.getQuery());
            ReadEntryListResponse response = GetEntryList.run(url, sessionId, moduleName, options.getSelectFields(), query, options.getMaxResult());

            if (response != null) {
                sugarRestResponse.setJsonRawRequest(response.getJsonRawRequest());
                sugarRestResponse.setJsonRawResponse(response.getJsonRawResponse());
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    List<Object> entities = response.getEntities();
                    String jsonEnitities = mapper.writeValueAsString(entities);
                    sugarRestResponse.setJData(jsonEnitities);
                    sugarRestResponse.setStatusCode(response.getStatusCode());

                    // Serialize json data to concrete Pojo objects.
                    Class moduleClass = moduleInfo.type;
                    if (moduleClass != null) {
                        JavaType type = mapper.getTypeFactory().constructCollectionType(ArrayList.class, moduleClass) ;
                        Object pojoObjList = mapper.readValue(jsonEnitities,  type);
                        sugarRestResponse.setData(pojoObjList);
                    } else {
                        sugarRestResponse.setData(entities);
                    }

                } else {
                    sugarRestResponse.setError(response.getError());
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                    sugarRestResponse.setJData(StringUtils.EMPTY);
                    sugarRestResponse.setData(new ArrayList<Object>());
                }
            }

            return sugarRestResponse;
        } catch (Exception exception) {
            sugarRestResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            sugarRestResponse.setError(ErrorResponse.format(exception, StringUtils.EMPTY));
        } finally {
            Authentication.logout(request.getUrl(), loginResponse.sessionId);
        }

        return sugarRestResponse;
    }

    /**
     * Gets all entities by page. Page options set in request options.
     *
     *  @param request The request object.
     *  @param moduleInfo The entity model info.
     *  @return SugarRestResponse object.
     */
    public static SugarRestResponse executeGetPaged(SugarRestRequest request, ModuleInfo moduleInfo) {
        SugarRestResponse sugarRestResponse = new SugarRestResponse();
        LoginResponse loginResponse = new LoginResponse();
        ObjectMapper mapper = JsonObjectMapper.getMapper();

        try {
            LoginRequest loginRequest = new LoginRequest(request.getUrl(), request.getUsername(), request.getPassword());
            loginResponse = Authentication.login(loginRequest);

            String url = request.getUrl();
            String sessionId = loginResponse.sessionId;
            String moduleName = moduleInfo.name;
            Options options = request.getOptions();

            String query = ModuleInfoExt.getQuery(moduleInfo, options.getQueryPredicates(), options.getQuery());

            ReadEntryListResponse response = GetPagedEntryList.run(url, sessionId, moduleName, options.getSelectFields(), query, options.getCurrentPage(), options.getNumberPerPage());

            if (response != null) {
                sugarRestResponse.setJsonRawRequest(response.getJsonRawRequest());
                sugarRestResponse.setJsonRawResponse(response.getJsonRawResponse());
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    List<Object> entities = response.getEntities();
                    String jsonEnitities = mapper.writeValueAsString(entities);
                    sugarRestResponse.setJData(jsonEnitities);
                    sugarRestResponse.setStatusCode(response.getStatusCode());

                    // Serialize json data to concrete Pojo objects.
                    Class moduleClass = moduleInfo.type;
                    if (moduleClass != null) {
                        JavaType type = mapper.getTypeFactory().constructCollectionType(ArrayList.class, moduleClass) ;
                        Object pojoObjList = mapper.readValue(jsonEnitities,  type);
                        sugarRestResponse.setData(pojoObjList);
                    } else {
                        sugarRestResponse.setData(entities);
                    }
                } else {
                    sugarRestResponse.setError(response.getError());
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                    sugarRestResponse.setJData(StringUtils.EMPTY);
                    sugarRestResponse.setData(new ArrayList<Object>());
                }
            }

            return sugarRestResponse;
        }
        catch (Exception exception) {
            sugarRestResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            sugarRestResponse.setError(ErrorResponse.format(exception, StringUtils.EMPTY));
        } finally {
            Authentication.logout(request.getUrl(), loginResponse.sessionId);
        }

        return sugarRestResponse;
    }

    /**
     * Insert entity.
     *
     *  @param request The request object.
     *  @param moduleInfo The entity model info.
     *  @return SugarRestResponse object.
     */
    public static SugarRestResponse executeInsert(SugarRestRequest request, ModuleInfo moduleInfo) {
        SugarRestResponse sugarRestResponse = new SugarRestResponse();
        LoginResponse loginResponse = new LoginResponse();
        ObjectMapper mapper = JsonObjectMapper.getMapper();

        try {
            LoginRequest loginRequest = new LoginRequest(request.getUrl(), request.getUsername(), request.getPassword());
            loginResponse = Authentication.login(loginRequest);

            String url = request.getUrl();
            String sessionId = loginResponse.sessionId;
            String moduleName = moduleInfo.name;
            Options options = request.getOptions();
            Object entity = request.getParameter();

            InsertEntryResponse response = InsertEntry.run(url, sessionId, moduleName, entity, options.getSelectFields());
            if (response != null) {
                sugarRestResponse.setJsonRawRequest(response.getJsonRawRequest());
                sugarRestResponse.setJsonRawResponse(response.getJsonRawResponse());
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    String insertedId = response.id;
                    sugarRestResponse.setData(insertedId);
                    String jsonEnitity = mapper.writeValueAsString(insertedId);
                    sugarRestResponse.setJData(jsonEnitity);
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                } else {
                    sugarRestResponse.setError(response.getError());
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                    sugarRestResponse.setJData(StringUtils.EMPTY);
                    sugarRestResponse.setData(StringUtils.EMPTY);
                }
            }

            return sugarRestResponse;
        } catch (Exception exception) {
            sugarRestResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            sugarRestResponse.setError(ErrorResponse.format(exception, StringUtils.EMPTY));
        } finally {
            Authentication.logout(request.getUrl(), loginResponse.sessionId);
        }

        return sugarRestResponse;
    }

    /**
     * Insert entities.
     *
     *  @param request The request object.
     *  @param moduleInfo The entity model info.
     *  @return SugarRestResponse object.
     */
    public static SugarRestResponse executeInserts(SugarRestRequest request, ModuleInfo moduleInfo) {
        SugarRestResponse sugarRestResponse = new SugarRestResponse();
        LoginResponse loginResponse = new LoginResponse();
        ObjectMapper mapper = JsonObjectMapper.getMapper();

        try {
            LoginRequest loginRequest = new LoginRequest(request.getUrl(), request.getUsername(), request.getPassword());
            loginResponse = Authentication.login(loginRequest);

            String url = request.getUrl();
            String sessionId = loginResponse.sessionId;
            String moduleName = moduleInfo.name;
            Options options = request.getOptions();
            List<Object> entities = (List<Object>)request.getParameter();

            InsertEntriesResponse response = InsertEntries.run(url, sessionId, moduleName, entities, options.getSelectFields());
            if (response != null) {
                sugarRestResponse.setJsonRawRequest(response.getJsonRawRequest());
                sugarRestResponse.setJsonRawResponse(response.getJsonRawResponse());
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    List<String> insertedIds = response.ids;
                    sugarRestResponse.setData(insertedIds);
                    String jsonEnitity = mapper.writeValueAsString(insertedIds);
                    sugarRestResponse.setJData(jsonEnitity);
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                } else {
                    sugarRestResponse.setError(response.getError());
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                    sugarRestResponse.setJData(StringUtils.EMPTY);
                    sugarRestResponse.setData(StringUtils.EMPTY);
                }
            }

            return sugarRestResponse;
        } catch (Exception exception) {
            sugarRestResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            sugarRestResponse.setError(ErrorResponse.format(exception, StringUtils.EMPTY));
        } finally {
            Authentication.logout(request.getUrl(), loginResponse.sessionId);
        }

        return sugarRestResponse;
    }

    /**
     * Update entity.
     *
     *  @param request The request object.
     *  @param moduleInfo The entity model info.
     *  @return SugarRestResponse object.
     */
    public static SugarRestResponse executeUpdate(SugarRestRequest request, ModuleInfo moduleInfo) {
        SugarRestResponse sugarRestResponse = new SugarRestResponse();
        LoginResponse loginResponse = new LoginResponse();
        ObjectMapper mapper = JsonObjectMapper.getMapper();

        try {
            LoginRequest loginRequest = new LoginRequest(request.getUrl(), request.getUsername(), request.getPassword());
            loginResponse = Authentication.login(loginRequest);

            String url = request.getUrl();
            String sessionId = loginResponse.sessionId;
            String moduleName = moduleInfo.name;
            Options options = request.getOptions();
            Object entity = request.getParameter();

            UpdateEntryResponse response = UpdateEntry.run(url, sessionId, moduleName, entity, options.getSelectFields());
            if (response != null) {
                sugarRestResponse.setJsonRawRequest(response.getJsonRawRequest());
                sugarRestResponse.setJsonRawResponse(response.getJsonRawResponse());
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    String updatedId = response.id;
                    sugarRestResponse.setData(updatedId);
                    String jsonEnitity = mapper.writeValueAsString(updatedId);
                    sugarRestResponse.setJData(jsonEnitity);
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                } else {
                    sugarRestResponse.setError(response.getError());
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                    sugarRestResponse.setJData(StringUtils.EMPTY);
                    sugarRestResponse.setData(StringUtils.EMPTY);
                }
            }

            return sugarRestResponse;
        } catch (Exception exception) {
            sugarRestResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            sugarRestResponse.setError(ErrorResponse.format(exception, StringUtils.EMPTY));
        } finally {
            Authentication.logout(request.getUrl(), loginResponse.sessionId);
        }

        return sugarRestResponse;
    }

    /**
     * Insert entity.
     *
     *  @param request The request object.
     *  @param moduleInfo The entity model info.
     *  @return SugarRestResponse object.
     */
    public static SugarRestResponse executeInsertLinked(
            SugarRestRequest request, ModuleInfo moduleInfo) {
        SugarRestResponse sugarRestResponse = new SugarRestResponse();
        LoginResponse loginResponse = new LoginResponse();
        ObjectMapper mapper = JsonObjectMapper.getMapper();

        try {
            LoginRequest loginRequest = new LoginRequest(request.getUrl(),
                    request.getUsername(), request.getPassword());
            loginResponse = Authentication.login(loginRequest);

            String url = request.getUrl();
            String sessionId = loginResponse.sessionId;
            String moduleName = moduleInfo.name;
            Options options = request.getOptions();
            Object entity = request.getParameter();



            UpdateEntryResponse response = InsertLinkedEntry.run(url, sessionId,
                    moduleName, entity, options.getSelectFields());
            if (response != null) {
                sugarRestResponse
                        .setJsonRawRequest(response.getJsonRawRequest());
                sugarRestResponse
                        .setJsonRawResponse(response.getJsonRawResponse());
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    String updatedId = response.id;
                    sugarRestResponse.setData(updatedId);
                    String jsonEnitity = mapper.writeValueAsString(updatedId);
                    sugarRestResponse.setJData(jsonEnitity);
                    sugarRestResponse.setStatusCode(response.getStatusCode());

                    // Now update the linked modules and after that update
                    // relations
                    Map<Object, List<String>> linkedModules = request
                            .getOptions().getLinkedModules();

                    // TODO: Improve this code, lots of nested blocks
                    for (Object linkedClass : safeSet(linkedModules.keySet())) {
                        if (linkedClass instanceof Type) {
                            ModuleInfo mInfo = ModuleInfo
                                    .create((Type) linkedClass, null);
                            for (ModuleProperty property : moduleInfo.modelProperties) {
                                if (mInfo.jsonName.equals(property.jsonName)) {
                                    if (Collection.class.isAssignableFrom(property.type)) {
                                        Field field = entity
                                                .getClass()
                                                .getDeclaredField(
                                                        property.name);
                                        // For accessing private fields
                                        field.setAccessible(true);
                                        Collection c = (Collection) field
                                                .get(entity);
                                        
                                        for (Object o : c) {
                                            UpdateEntryResponse response1 = InsertLinkedEntry
                                                    .run(url, sessionId,
                                                            mInfo.name,
                                                            o,
                                                            options.getSelectFields());

                                            // InsertRelationshipResponse
                                            // com.sugaronrest.restapicalls.methodcalls.InsertRelationship.run(String
                                            // url, String sessionId, String
                                            // moduleName, String moduleId,
                                            // String linkFieldName,
                                            // List<String>
                                            // relatedIds, List<String>
                                            // selectFields)

                                            if (response1
                                                    .getStatusCode() == HttpStatus.SC_OK) {
                                                InsertRelationship.run(url,
                                                        sessionId, moduleName,
                                                        updatedId,
                                                        mInfo.jsonName,
                                                        Arrays.asList(
                                                                response1.id),
                                                        options.getSelectFields());
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }

                } else {
                    sugarRestResponse.setError(response.getError());
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                    sugarRestResponse.setJData(StringUtils.EMPTY);
                    sugarRestResponse.setData(StringUtils.EMPTY);
                }

            }

            return sugarRestResponse;
        } catch (Exception exception) {
            sugarRestResponse
                    .setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            sugarRestResponse.setError(
                    ErrorResponse.format(exception, StringUtils.EMPTY));
        } finally {
            Authentication.logout(request.getUrl(), loginResponse.sessionId);
        }

        return sugarRestResponse;
    }

    /**
     * Insert entity.
     *
     *  @param request The request object.
     *  @param moduleInfo The entity model info.
     *  @return SugarRestResponse object.
     */
    public static SugarRestResponse executeInsertRelationship(
            SugarRestRequest request, ModuleInfo moduleInfo) {
        SugarRestResponse sugarRestResponse = new SugarRestResponse();
        LoginResponse loginResponse = new LoginResponse();
        ObjectMapper mapper = JsonObjectMapper.getMapper();

        try {
            LoginRequest loginRequest = new LoginRequest(request.getUrl(),
                    request.getUsername(), request.getPassword());
            loginResponse = Authentication.login(loginRequest);

            String url = request.getUrl();
            String sessionId = loginResponse.sessionId;
            String moduleName = moduleInfo.name;

            OptionsRelationship options = (OptionsRelationship) request.getOptions();
            String moduleId = request.getParameter().toString();

            InsertRelationshipResponse response = InsertRelationship.run(url,
                    sessionId,
                    moduleName, moduleId, options.getLinkFieldName(),
                    options.getRelatedIds(), options.getSelectFields());
            if (response != null) {
                sugarRestResponse
                        .setJsonRawRequest(response.getJsonRawRequest());
                sugarRestResponse
                        .setJsonRawResponse(response.getJsonRawResponse());
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    String insertedId = response.id;
                    sugarRestResponse.setData(insertedId);
                    String jsonEnitity = mapper.writeValueAsString(insertedId);
                    sugarRestResponse.setJData(jsonEnitity);
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                } else {
                    sugarRestResponse.setError(response.getError());
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                    sugarRestResponse.setJData(StringUtils.EMPTY);
                    sugarRestResponse.setData(StringUtils.EMPTY);
                }

            }

            return sugarRestResponse;
        } catch (Exception exception) {
            sugarRestResponse
                    .setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            sugarRestResponse.setError(
                    ErrorResponse.format(exception, StringUtils.EMPTY));
        } finally {
            Authentication.logout(request.getUrl(), loginResponse.sessionId);
        }

        return sugarRestResponse;
    }

    /**
     * Update entities.
     *
     *  @param request The request object.
     *  @param moduleInfo The entity model info.
     *  @return SugarRestResponse object.
     */
    public static SugarRestResponse executeUpdates(SugarRestRequest request, ModuleInfo moduleInfo) {
        SugarRestResponse sugarRestResponse = new SugarRestResponse();
        LoginResponse loginResponse = new LoginResponse();
        ObjectMapper mapper = JsonObjectMapper.getMapper();

        try {
            LoginRequest loginRequest = new LoginRequest(request.getUrl(), request.getUsername(), request.getPassword());
            loginResponse = Authentication.login(loginRequest);

            String url = request.getUrl();
            String sessionId = loginResponse.sessionId;
            String moduleName = moduleInfo.name;
            Options options = request.getOptions();
            List<Object> entities = (List<Object>)request.getParameter();

            UpdateEntriesResponse response = UpdateEntries.run(url, sessionId, moduleName, entities, options.getSelectFields());
            if (response != null) {
                sugarRestResponse.setJsonRawRequest(response.getJsonRawRequest());
                sugarRestResponse.setJsonRawResponse(response.getJsonRawResponse());
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    List<String> updatedIds = response.ids;
                    sugarRestResponse.setData(updatedIds);
                    String jsonEnitity = mapper.writeValueAsString(updatedIds);
                    sugarRestResponse.setJData(jsonEnitity);
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                } else {
                    sugarRestResponse.setError(response.getError());
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                    sugarRestResponse.setJData(StringUtils.EMPTY);
                    sugarRestResponse.setData(StringUtils.EMPTY);
                }
            }

            return sugarRestResponse;
        } catch (Exception exception) {
            sugarRestResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            sugarRestResponse.setError(ErrorResponse.format(exception, StringUtils.EMPTY));
        } finally {
            Authentication.logout(request.getUrl(), loginResponse.sessionId);
        }

        return sugarRestResponse;
    }

    /**
     * Delete entity.
     *
     *  @param request The request object.
     *  @param moduleInfo The entity model info.
     *  @return SugarRestResponse object.
     */
    public static SugarRestResponse executeDelete(SugarRestRequest request, ModuleInfo moduleInfo) {
        SugarRestResponse sugarRestResponse = new SugarRestResponse();
        LoginResponse loginResponse = new LoginResponse();
        ObjectMapper mapper = JsonObjectMapper.getMapper();

        try {
            LoginRequest loginRequest = new LoginRequest(request.getUrl(), request.getUsername(), request.getPassword());
            loginResponse = Authentication.login(loginRequest);

            String url = request.getUrl();
            String sessionId = loginResponse.sessionId;
            String moduleName = moduleInfo.name;
            String id = (String)request.getParameter();

            DeleteEntryResponse response = DeleteEntry.run(url, sessionId, moduleName, id);
            if (response != null) {
                sugarRestResponse.setJsonRawRequest(response.getJsonRawRequest());
                sugarRestResponse.setJsonRawResponse(response.getJsonRawResponse());
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    String deletedId = response.id;
                    sugarRestResponse.setData(deletedId);
                    String jsonEnitity = mapper.writeValueAsString(deletedId);
                    sugarRestResponse.setJData(jsonEnitity);
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                } else {
                    sugarRestResponse.setError(response.getError());
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                    sugarRestResponse.setJData(StringUtils.EMPTY);
                    sugarRestResponse.setData(StringUtils.EMPTY);
                }
            }

            return sugarRestResponse;
        } catch (Exception exception) {
            sugarRestResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            sugarRestResponse.setError(ErrorResponse.format(exception, StringUtils.EMPTY));
        } finally {
            Authentication.logout(request.getUrl(), loginResponse.sessionId);
        }

        return sugarRestResponse;
    }

    /**
     * Gets entity by id - returned data with linked modules data.
     *
     *  @param request The request object.
     *  @param moduleInfo The entity model info.
     *  @return SugarRestResponse object.
     */
    public static SugarRestResponse executeLinkedGetById(SugarRestRequest request, ModuleInfo moduleInfo) {
        SugarRestResponse sugarRestResponse = new SugarRestResponse();
        LoginResponse loginResponse = new LoginResponse();
        ObjectMapper mapper = JsonObjectMapper.getMapper();

        try {
            LoginRequest loginRequest = new LoginRequest(request.getUrl(), request.getUsername(), request.getPassword());
            loginResponse = Authentication.login(loginRequest);

            String url = request.getUrl();
            String sessionId = loginResponse.sessionId;
            String moduleName = moduleInfo.name;
            Options options = request.getOptions();
            String identifier = (String)request.getParameter();
            List<Object> linkedSelectFields = ModuleInfoExt.getJsonLinkedInfo(options.getLinkedModules());

            ReadLinkedEntryResponse response = GetLinkedEntry.run(url, sessionId, moduleName, identifier, options.getSelectFields(), linkedSelectFields);
            if (response != null) {
                sugarRestResponse.setJsonRawRequest(response.getJsonRawRequest());
                sugarRestResponse.setJsonRawResponse(response.getJsonRawResponse());
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    Object entity = response.getEntity();
                    String jsonEnitity = mapper.writeValueAsString(entity);
                    sugarRestResponse.setJData(jsonEnitity);
                    sugarRestResponse.setStatusCode(response.getStatusCode());

                    // The actual data is set to null. To retrieve data use custom classes with linked modules.
                    sugarRestResponse.setData(null);
                } else {
                    sugarRestResponse.setError(response.getError());
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                    sugarRestResponse.setJData(StringUtils.EMPTY);
                    sugarRestResponse.setData(null);
                }
            }

            return sugarRestResponse;
        } catch (Exception exception) {
            sugarRestResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            sugarRestResponse.setError(ErrorResponse.format(exception, StringUtils.EMPTY));
        } finally {
            Authentication.logout(request.getUrl(), loginResponse.sessionId);
        }

        return sugarRestResponse;
    }

    /**
     * Gets all linked entities limited by MaxResultCount sets in request options.
     *
     *  @param request The request object.
     *  @param moduleInfo The entity model info.
     *  @return SugarRestResponse object.
     */
    public static SugarRestResponse executeLinkedGetAll(SugarRestRequest request, ModuleInfo moduleInfo) {
        SugarRestResponse sugarRestResponse = new SugarRestResponse();
        LoginResponse loginResponse = new LoginResponse();
        ObjectMapper mapper = JsonObjectMapper.getMapper();

        try {
            LoginRequest loginRequest = new LoginRequest(request.getUrl(), request.getUsername(), request.getPassword());
            loginResponse = Authentication.login(loginRequest);

            String url = request.getUrl();
            String sessionId = loginResponse.sessionId;
            String moduleName = moduleInfo.name;
            Options options = request.getOptions();
            List<Object> linkedSelectFields = ModuleInfoExt.getJsonLinkedInfo(options.getLinkedModules());
            String query = ModuleInfoExt.getQuery(moduleInfo, options.getQueryPredicates(), options.getQuery());

            ReadLinkedEntryListResponse response = GetLinkedEntryList.run(url, sessionId, moduleName, options.getSelectFields(), linkedSelectFields, query, options.getMaxResult());

            if (response != null) {
                sugarRestResponse.setJsonRawRequest(response.getJsonRawRequest());
                sugarRestResponse.setJsonRawResponse(response.getJsonRawResponse());
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    List<Object> entities = response.getEntities();
                    String jsonEnitities = mapper.writeValueAsString(entities);
                    sugarRestResponse.setJData(jsonEnitities);
                    sugarRestResponse.setStatusCode(response.getStatusCode());

                    // The actual data is set to null. To retrieve data use custom classes with linked modules.
                    sugarRestResponse.setData(null);
                } else {
                    sugarRestResponse.setError(response.getError());
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                    sugarRestResponse.setJData(StringUtils.EMPTY);
                    sugarRestResponse.setData(null);
                }
            }

            return sugarRestResponse;
        } catch (Exception exception) {
            sugarRestResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            sugarRestResponse.setError(ErrorResponse.format(exception, StringUtils.EMPTY));
        } finally {
            Authentication.logout(request.getUrl(), loginResponse.sessionId);
        }

        return sugarRestResponse;
    }

    /**
     * Gets available modules.
     *
     *  @param request The request object.
     *  @param moduleInfo The entity model info.
     *  @return SugarRestResponse object.
     */
    public static SugarRestResponse executeGetAvailableModules(SugarRestRequest request, ModuleInfo moduleInfo) {
        SugarRestResponse sugarRestResponse = new SugarRestResponse();
        LoginResponse loginResponse = new LoginResponse();
        ObjectMapper mapper = JsonObjectMapper.getMapper();

        try {
            LoginRequest loginRequest = new LoginRequest(request.getUrl(), request.getUsername(), request.getPassword());
            loginResponse = Authentication.login(loginRequest);

            ReadAvailableModulesResponse response = GetAvailableModules.run(request.getUrl(), loginResponse.sessionId);
            if (response != null)
            {
                sugarRestResponse.setJsonRawRequest(response.getJsonRawRequest());
                sugarRestResponse.setJsonRawResponse(response.getJsonRawResponse());
                if (response.getStatusCode() == HttpStatus.SC_OK)
                {
                    List<String> entities = response.getModules();
                    String jsonEnitities = mapper.writeValueAsString(entities);
                    sugarRestResponse.setJData(jsonEnitities);
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                    sugarRestResponse.setData(entities);
                }
                else
                {
                    sugarRestResponse.setError(response.getError());
                    sugarRestResponse.setStatusCode(response.getStatusCode());
                    sugarRestResponse.setJData(StringUtils.EMPTY);
                    sugarRestResponse.setData(new ArrayList<String>());
                }
            }

            return sugarRestResponse;
        }
        catch (Exception exception) {
            sugarRestResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            sugarRestResponse.setError(ErrorResponse.format(exception, StringUtils.EMPTY));
        }
        finally {
            Authentication.logout(request.getUrl(), loginResponse.sessionId);
        }
        return sugarRestResponse;
    }

    public static <T> Set<T> safeSet(Set<T> other) {
        return other == null ? Collections.EMPTY_SET : other;
    }

}
