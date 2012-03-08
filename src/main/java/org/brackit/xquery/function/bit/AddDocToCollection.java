/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Brackit Project Team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.function.bit;

import org.brackit.annotation.FunctionAnnotation;
import org.brackit.annotation.ModuleAnnotation;
import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.Namespaces;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.node.parser.CollectionParser;
import org.brackit.xquery.node.parser.SequenceParser;
import org.brackit.xquery.node.parser.SubtreeParser;
import org.brackit.xquery.xdm.Collection;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.Store;

/**
 * 
 * @author Henrique Valer
 * 
 */
@ModuleAnnotation(description = "A module for performing various operations "
		+ "on the Brackit Database.")
@FunctionAnnotation(description = "Adds a document into a collection. If the "
		+ "specified collection does not exist, it will be automatically created. ", parameters = {
		"$collectionName", "$documentName" })
public class AddDocToCollection extends AbstractFunction {

	public static final QNm DEFAULT_NAME = new QNm(Namespaces.BIT_NSURI,
			Namespaces.BIT_PREFIX, "add-doc-to-collection");

	public AddDocToCollection(QNm name, Signature signature) {
		super(name, signature, true);
	}

	@Override
	public Sequence execute(StaticContext sctx, QueryContext ctx,
			Sequence[] args) throws QueryException {
		try {
			String collName = ((Atomic) args[0]).stringValue();
			SubtreeParser parser = new CollectionParser(new SequenceParser(
					args[1]));

			Store s = ctx.getStore();
			Collection<?> coll = null;
			try {
				coll = s.lookup(collName);
				coll.add(parser);
			} catch (DocumentException e) {
				// collection does not exist
				coll = s.create(collName, parser);
			}
			return Bool.TRUE;
		} catch (Exception e) {
			throw new QueryException(e,
					ErrorCode.BIT_ADDTOCOLLECTION_INT_ERROR, e.getMessage());
		}
	}
}