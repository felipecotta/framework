/*
 * Demoiselle Framework
 *
 * License: GNU Lesser General Public License (LGPL), version 3 or later.
 * See the lgpl.txt file in the root directory or <https://www.gnu.org/licenses/lgpl.html>.
 */
package org.demoiselle.jee.crud;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.demoiselle.jee.core.api.crud.Result;
import org.demoiselle.jee.crud.pagination.DemoisellePaginationConfig;
import org.demoiselle.jee.crud.pagination.DemoisellePaginationMessage;

/**
 * TODO javadoc
 *
 * @author SERPRO
 *
 */
@Provider
public class CrudFilter implements ContainerResponseFilter, ContainerRequestFilter {

	@Context
	private ResourceInfo info;

	@Context
	private UriInfo uriInfo;

	@Inject
	private DemoiselleRequestContext drc;

	@Inject
	private DemoisellePaginationConfig paginationConfig;

	private final String DEFAULT_RANGE_KEY = "range";
	private final String HTTP_HEADER_CONTENT_RANGE = "Content-Range";
	private final String HTTP_HEADER_ACCEPT_RANGE = "Accept-Range";

	@Inject
	private DemoisellePaginationMessage message;

	private static final Logger logger = Logger.getLogger(CrudFilter.class.getName());

	public CrudFilter() {
	}

	 public CrudFilter(UriInfo uriInfo, ResourceInfo info, DemoiselleRequestContext drc, DemoisellePaginationConfig dpc, DemoisellePaginationMessage message) {
    	 this.uriInfo = uriInfo;
    	 this.info = info;
    	 this.drc = drc;
    	 this.paginationConfig = dpc;
    	 this.message = message;
	 }

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {

		if (isRequestPagination()) {
			try {
				checkAndFillRangeValues();				
			} catch (IllegalArgumentException e) {
				throw new BadRequestException(e.getMessage());
			}
		}
		
		fillFieldsToFilter();

	}

    private void fillFieldsToFilter() {
        uriInfo.getQueryParameters().forEach((key, values) ->{
            if(!isReservedKey(key)){
                Set<String> paramValues = new HashSet<>();
                
                values.forEach(value -> {
                    String[] paramValueSplit = value.split("\\,");
                    paramValues.addAll(Arrays.asList(paramValueSplit));
                });
                
                drc.getFieldsFilter().put(key, paramValues);
            }
        });
    }

    private Boolean isReservedKey(String key) {
        return key.equals(DEFAULT_RANGE_KEY);
    }

    @Override
	public void filter(ContainerRequestContext req, ContainerResponseContext response) throws IOException {

		if (response.getEntity() instanceof Result) {

			buildHeaders().forEach((k, v) -> response.getHeaders().putSingle(k, v));

			response.setEntity(((Result) response.getEntity()).getContent());

			if (!isPartialContentResponse()) {
				response.setStatus(Status.OK.getStatusCode());
			} else {
				response.setStatus(Status.PARTIAL_CONTENT.getStatusCode());
			}
		} else {
			if (Status.BAD_REQUEST.getStatusCode() == response.getStatus() && drc.getEntityClass() == null) {
				response.getHeaders().putSingle(HTTP_HEADER_ACCEPT_RANGE, buildAcceptRange());
			}
		}

	}

	private Boolean isPartialContentResponse() {
		return !((drc.getLimit() + 1) >= drc.getCount());
	}

	private Map<String, String> buildHeaders() {
	    Map<String, String> headers = new HashMap<>();
		
	    headers.put(HTTP_HEADER_CONTENT_RANGE, buildContentRange());
	    headers.put(HTTP_HEADER_ACCEPT_RANGE, buildAcceptRange());
	    String linkHeader = buildLinkHeader();
	    
	    if(!linkHeader.isEmpty()){
	        headers.put(HttpHeaders.LINK, linkHeader);
	    }

		return headers;
	}

	private String buildLinkHeader() {
		StringBuffer sb = new StringBuffer();
		String url = uriInfo.getRequestUri().toString();
		url = url.replaceFirst(".range=.*", "");

		if (drc.getOffset().equals(0) && drc.getLimit() == null) {
			drc.setLimit(paginationConfig.getDefaultPagination()-1);
		}

		Integer offset = drc.getOffset() + 1;
		Integer limit = drc.getLimit() + 1;
		Integer quantityPerPage = (limit - offset) + 1;
		
		if(uriInfo.getQueryParameters().isEmpty() 
		        || (uriInfo.getQueryParameters().size() == 1 && uriInfo.getQueryParameters().containsKey(DEFAULT_RANGE_KEY))){
		    url += "?" + DEFAULT_RANGE_KEY + "=";
	    }
		else{
		    url += "&" + DEFAULT_RANGE_KEY + "=";
		}
		
		if (!isFirstPage()) {
		    Integer prevPageRangeInit = (drc.getOffset() - quantityPerPage) < 0 ? 0 : (drc.getOffset() - quantityPerPage);
		    Integer firstRange2 = quantityPerPage - 1 < drc.getOffset() - 1 ? quantityPerPage - 1 : drc.getOffset() - 1;
		            
			String firstPage = url + 0 + "-" + firstRange2;			
			String prevPage = url + prevPageRangeInit + "-" + (drc.getOffset() - 1);

			sb.append("<").append(firstPage).append(">; rel=\"first\",");
			sb.append("<").append(prevPage).append(">; rel=\"prev\",");
		}

		if (isPartialContentResponse()) {
			String nextPage = url + (drc.getOffset() + quantityPerPage) + "-" + (2 * quantityPerPage + drc.getOffset() - 1);
			String lastPage = url + (drc.getCount() - quantityPerPage) + "-" + (drc.getCount() - 1);
			
			if( offset + quantityPerPage >= drc.getCount() - 1){
			    nextPage = lastPage;
			}

			sb.append("<").append(nextPage).append(">; rel=\"next\",");
			sb.append("<").append(lastPage).append(">; rel=\"last\"");
		}

		return sb.toString();
	}

	private Boolean isFirstPage() {
		return drc.getOffset().equals(0);
	}

	private String buildContentRange() {
	    Integer limit = drc.getLimit() == null ? 0 : drc.getLimit();
		return drc.getOffset() + "-" + (limit.equals(0) ? drc.getCount() - 1 : drc.getLimit()) + "/" + drc.getCount();
	}

	private String buildAcceptRange() {
		String resource = "";

		if (drc.getEntityClass() != null) {
			resource = drc.getEntityClass().getSimpleName().toLowerCase();
		} else {
			if (info.getResourceClass() != null) {
				Class<?> targetClass = info.getResourceClass();
				if (targetClass.getSuperclass().equals(AbstractREST.class)) {
					Class<?> type = (Class<?>) ((ParameterizedType) targetClass.getGenericSuperclass())
							.getActualTypeArguments()[0];
					resource = type.getSimpleName().toLowerCase();
				}
			}
		}

		return resource + " " + paginationConfig.getDefaultPagination();
	}

	private void checkAndFillRangeValues() throws IllegalArgumentException {
		List<String> rangeList = uriInfo.getQueryParameters().get(DEFAULT_RANGE_KEY);
		if (!rangeList.isEmpty()) {
			String range[] = rangeList.get(0).split("-");
			if (range.length == 2) {
				String offset = range[0];
				String limit = range[1];

				try {
					drc.setOffset(new Integer(offset));
					drc.setLimit(new Integer(limit));

					if (drc.getOffset() > drc.getLimit()) {
						logInvalidRangeParameters(rangeList.get(0));
						throw new IllegalArgumentException(this.message.invalidRangeParameters());
					}

					if (((drc.getLimit() - drc.getOffset()) + 1) > paginationConfig
							.getDefaultPagination()) {
						logger.warning(message.defaultPaginationNumberExceed(paginationConfig.getDefaultPagination())
								+ ", [" + drc.toString() + "]");
						throw new IllegalArgumentException(
								message.defaultPaginationNumberExceed(paginationConfig.getDefaultPagination()));
					}
					
					if(drc.getLimit().equals(0) && isRequestPagination()){
					    drc.setLimit(null);
					}

				} catch (NumberFormatException nfe) {
					logInvalidRangeParameters(rangeList.get(0));
					throw new IllegalArgumentException(message.invalidRangeParameters());
				}
			} else {
				logInvalidRangeParameters(rangeList.get(0));
				throw new IllegalArgumentException(message.invalidRangeParameters());
			}
		}

	}

	private void logInvalidRangeParameters(String range) {
		logger.warning(message.invalidRangeParameters() + ", [params: " + range + "]");
	}

	private Boolean isRequestPagination() {
		// Verify if contains 'range' in url
		if (uriInfo.getQueryParameters().containsKey(DEFAULT_RANGE_KEY)) {
			return true;
		}
		return false;
	}

}