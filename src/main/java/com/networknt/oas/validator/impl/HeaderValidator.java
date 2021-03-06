/*******************************************************************************
 *  Copyright (c) 2017 ModelSolv, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     ModelSolv, Inc. - initial API and implementation and/or initial documentation
 *******************************************************************************/
package com.networknt.oas.validator.impl;

import com.networknt.oas.model.*;
import com.networknt.oas.validator.ObjectValidatorBase;
import com.networknt.oas.validator.ValidationResults;
import com.networknt.oas.validator.Validator;
import com.networknt.service.SingletonServiceFactory;

import static com.networknt.oas.validator.Messages.m;

public class HeaderValidator extends ObjectValidatorBase<Header> {

	private static Validator<Schema> schemaValidator = SingletonServiceFactory.getBean(Validator.class, Schema.class);
	private static Validator<MediaType> mediaTypeValidator = SingletonServiceFactory.getBean(Validator.class, MediaType.class);

	@Override
	public void validateObject(Header header, ValidationResults results) {
		// no validations for: description, deprecated, allowEmptyValue, explode,
		// example, examples
		validateString(header.getName(false), results, false, "name");
		validateString(header.getIn(false), results, false, Regexes.PARAM_IN_REGEX, "in");
		checkPathParam(header, results);
		checkRequired(header, results);
		validateString(header.getStyle(false), results, false, Regexes.STYLE_REGEX, "style");
		checkAllowReserved(header, results);
		// TODO Q: Should schema be required in header object?
		validateField(header.getSchema(false), results, false, "schema", schemaValidator);
		validateMap(header.getContentMediaTypes(false), results, false, "content", Regexes.NOEXT_REGEX,
				mediaTypeValidator);
		validateExtensions(header.getExtensions(false), results);
	}

	private void checkPathParam(Header header, ValidationResults results) {
		if (header.getIn(false) != null && header.getIn(false).equals("path") && header.getName(false) != null) {
			String path = getPathString(header);
			if (path != null) {
				if (!path.matches(".*/\\{" + header.getName(false) + "\\}(/.*)?")) {
					results.addError(m.msg("MissingPathTplt|No template for path parameter in path string",
							header.getName(false), path), "name");
				}
			} else {
				results.addWarning(m.msg("NoPath|Could not locate path for parameter", header.getName(false),
						header.getIn(false)));
			}
		}
	}

	private void checkRequired(Header header, ValidationResults results) {
		if ("path".equals(header.getIn(false))) {
			if (header.getRequired(false) != Boolean.TRUE) {
				results.addError(
						m.msg("PathParamReq|Path param must have 'required' property set true", header.getName(false)),
						"required");
			}
		}
	}

	private void checkAllowReserved(Header header, ValidationResults results) {
		if (header.isAllowReserved() && !"query".equals(header.getIn(false))) {
			results.addWarning(m.msg("NonQryAllowRsvd|AllowReserved is ignored for non-query parameter",
					header.getName(false), header.getIn(false)), "allowReserved");
		}
	}

	private String getPathString(Header header) {
		OpenApiObject<OpenApi3, ?> parent = (OpenApiObject<OpenApi3, ?>) header.getParentObject();
		while (parent != null && !(parent instanceof Path)) {
			parent = (OpenApiObject<OpenApi3, ?>) parent.getParentObject();
		}
		return parent instanceof Path ? parent.getPathInParent() : null;
	}
}
