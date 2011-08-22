/*
 * [New BSD License]
 * Copyright (c) 2011, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.compiler.parser;

import java.util.Arrays;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;

/**
 * Straight-forward, recursive descent parser.
 * 
 * @author Sebastian Baechle
 * 
 */
public class XQParser extends Tokenizer {

	private class MismatchException extends QueryException {
		private MismatchException(String... expected) {
			super(ErrorCode.ERR_PARSING_ERROR, "Expected one of %s: '%s'",
					Arrays.toString(expected), paraphrase());
		}
	}

	private static final String[] RESERVED_FUNC_NAMES = new String[] {
			"attribute", "comment", "document-node", "element",
			"empty-sequence", "funtion", "if", "item", "namespace-node",
			"node", "processing-instruction", "schema-attribute",
			"schema-element", "switch", "text", "typeswitch" };

	private String encoding = "UTF-8";

	private String version = "3.0";

	private boolean update;

	private VarScopes variables;

	public XQParser(String query) {
		super(query);
	}

	public AST parse() throws QueryException {
		AST module = module();
		if (module == null) {
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"No module found");
		}
		consumeEOF();
		AST xquery = new AST(XQ.XQuery);
		xquery.addChild(module);
		return xquery;
	}

	public void setXQVersion(String version) throws QueryException {
		if ("3.0".equals(version)) {
			this.version = version;
		} else if ("1.0".equals(version)) {
			this.version = version;
		} else if ("1.1".equals(version)) {
			this.version = version;
		} else {
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"unsupported version: " + version);
		}
	}

	private void setEncoding(String encoding) {
		System.out.println("set encoding " + encoding);
	}

	private AST module() throws QueryException {
		versionDecl();
		AST module = libraryModule();
		if (module == null) {
			module = mainModule();
		}
		return module;
	}

	private boolean versionDecl() throws QueryException {
		if (!attemptSkipWS("xquery")) {
			return false;
		}
		boolean vDecl = false;
		boolean eDecl = false;
		if (attemptSkipWS("version")) {
			setXQVersion(stringLiteral(false, true).getValue());
			vDecl = true;
		}
		if (attemptSkipWS("encoding")) {
			setEncoding(stringLiteral(false, true).getValue());
			eDecl = true;
		}
		if ((!vDecl) && (!eDecl)) {
			throw new MismatchException("version", "encoding");
		}
		consumeSkipWS(";");
		return true;
	}

	private AST libraryModule() throws QueryException {
		Token la = laSkipWS("module");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "namespace");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST ncn = ncnameLiteral(false, true);
		consumeSkipWS("=");
		AST uriLiteral = uriLiteral(false, true);

		AST module = new AST(XQ.LibraryModule);
		AST nsDecl = new AST(XQ.NamespaceDeclaration);
		nsDecl.addChild(ncn);
		nsDecl.addChild(uriLiteral);
		AST prolog = prolog();
		if (prolog != null) {
			module.addChild(prolog);
		}
		return module;
	}

	private AST mainModule() throws QueryException {
		AST prolog = prolog();
		AST body = queryBody();
		AST module = new AST(XQ.MainModule);
		if (prolog != null) {
			module.addChild(prolog);
		}
		module.addChild(body);
		return module;
	}

	private AST prolog() throws QueryException {
		AST prolog = new AST(XQ.Prolog);
		while (true) {
			AST def = defaultNamespaceDecl();
			def = (def != null) ? def : setter();
			def = (def != null) ? def : namespaceDecl();
			def = (def != null) ? def : importDecl();
			if (def != null) {
				consumeSkipWS(";");
				prolog.addChild(def);
			} else {
				break;
			}
		}
		while (true) {
			AST def = contextItemDecl();
			def = (def != null) ? def : annotatedDecl();
			def = (def != null) ? def : optionDecl();
			if (def != null) {
				consumeSkipWS(";");
				prolog.addChild(def);
			} else {
				break;
			}
		}
		return prolog;
	}

	private AST defaultNamespaceDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "default");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST decl;
		if (attemptSkipWS("element")) {
			decl = new AST(XQ.DefaultElementNamespace);
		} else if (attemptSkipWS("function")) {
			decl = new AST(XQ.DefaultFunctionNamespace);
		} else {
			throw new MismatchException("element", "function");
		}
		consumeSkipWS("namespace");
		decl.addChild(uriLiteral(false, true));
		return decl;
	}

	private AST setter() throws QueryException {
		AST setter = boundarySpaceDecl();
		setter = (setter != null) ? setter : defaultCollationDecl();
		setter = (setter != null) ? setter : baseURIDecl();
		setter = (setter != null) ? setter : constructionDecl();
		setter = (setter != null) ? setter : orderingModeDecl();
		setter = (setter != null) ? setter : emptyOrderDecl();
		// Begin XQuery Update Facility 1.0
		setter = (setter != null) ? setter : revalidationDecl();
		// Begin XQuery Update Facility 1.0
		setter = (setter != null) ? setter : copyNamespacesDecl();
		setter = (setter != null) ? setter : decimalFormatDecl();
		return setter;
	}

	private AST boundarySpaceDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "boundary-space");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST decl = new AST(XQ.BoundarySpaceDeclaration);
		if (attemptSkipWS("preserve")) {
			decl.addChild(new AST(XQ.BoundarySpaceModePreserve));
		} else if (attemptSkipWS("strip")) {
			decl.addChild(new AST(XQ.BoundarySpaceModeStrip));
		} else {
			throw new MismatchException("preserve", "strip");
		}
		return decl;
	}

	private AST defaultCollationDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "default");
		if (la2 == null) {
			return null;
		}
		Token la3 = laSkipWS(la2, "collation");
		if (la3 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consume(la3);
		AST decl = new AST(XQ.CollationDeclaration);
		decl.addChild(uriLiteral(false, true));
		return decl;
	}

	private AST baseURIDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "base-uri");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST decl = new AST(XQ.BaseURIDeclaration);
		decl.addChild(uriLiteral(false, true));
		return decl;
	}

	private AST constructionDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "construction");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST decl = new AST(XQ.ConstructionDeclaration);
		if (attemptSkipWS("preserve")) {
			decl.addChild(new AST(XQ.ConstructionModePreserve));
		} else if (attemptSkipWS("strip")) {
			decl.addChild(new AST(XQ.ConstructionModeStrip));
		} else {
			throw new MismatchException("preserve", "strip");
		}
		return decl;
	}

	private AST orderingModeDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "ordering");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST decl = new AST(XQ.OrderingModeDeclaration);
		if (attemptSkipWS("ordered")) {
			decl.addChild(new AST(XQ.OrderingModeOrdered));
		} else if (attemptSkipWS("unordered")) {
			decl.addChild(new AST(XQ.OrderingModeUnordered));
		} else {
			throw new MismatchException("ordered", "unordered");
		}
		return decl;
	}

	private AST emptyOrderDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "default");
		if (la2 == null) {
			return null;
		}
		Token la3 = laSkipWS(la2, "order");
		if (la3 == null) {
			return null;
		}
		Token la4 = laSkipWS(la3, "empty");
		if (la4 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consume(la3);
		consume(la4);
		AST decl = new AST(XQ.EmptyOrderDeclaration);
		if (attemptSkipWS("greatest")) {
			decl.addChild(new AST(XQ.EmptyOrderModeGreatest));
		} else if (attemptSkipWS("least")) {
			decl.addChild(new AST(XQ.EmptyOrderModeLeast));
		} else {
			throw new MismatchException("greatest", "least");
		}
		return decl;
	}

	// Begin XQuery Update Facility 1.0
	private AST revalidationDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "revalidation");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST decl = new AST(XQ.RevalidationDeclaration);
		if (attemptSkipWS("strict")) {
			decl.addChild(new AST(XQ.RevalidationModeStrict));
		} else if (attemptSkipWS("lax")) {
			decl.addChild(new AST(XQ.RevalidationModeLax));
		} else if (attemptSkipWS("skip")) {
			decl.addChild(new AST(XQ.RevalidationModeSkip));
		} else {
			throw new MismatchException("strict", "lax", "skip");
		}
		return decl;
	}

	// End XQuery Update Facility 1.0

	private AST copyNamespacesDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "copy-namespaces");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST decl = new AST(XQ.CopyNamespacesDeclaration);
		decl.addChild(preserveMode());
		consumeSkipWS(",");
		decl.addChild(inheritMode());

		return decl;
	}

	private AST preserveMode() throws QueryException {
		if (attemptSkipWS("preserve")) {
			return new AST(XQ.CopyNamespacesPreserveModePreserve);
		} else if (attemptSkipWS("no-preserve")) {
			return new AST(XQ.CopyNamespacesPreserveModeNoPreserve);
		} else {
			throw new MismatchException("preserve", "no-preserve");
		}
	}

	private AST inheritMode() throws QueryException {
		if (attemptSkipWS("inherit")) {
			return new AST(XQ.CopyNamespacesInheritModeInherit);
		} else if (attemptSkipWS("no-inherit")) {
			return new AST(XQ.CopyNamespacesInheritModeNoInherit);
		} else {
			throw new MismatchException("inherit", "no-inherit");
		}
	}

	private AST decimalFormatDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "default");
		Token la3 = laSkipWS((la2 != null) ? la2 : la, "decimal-format");
		if (la3 == null) {
			return null;
		}
		consume(la);
		if (la2 != null) {
			consume(la2);
		}
		consume(la3);
		AST decl = new AST(XQ.DecimalFormatDeclaration);
		;
		AST[] dfProperties = new AST[0];
		AST dfPropertyName;
		while ((dfPropertyName = dfPropertyName()) != null) {
			consumeSkipWS("=");
			AST value = stringLiteral(false, true);
			AST dfp = new AST(XQ.DecimalFormatProperty);
			dfp.addChild(dfPropertyName);
			dfp.addChild(value);
			dfProperties = add(dfProperties, dfp);
		}
		decl.addChildren(dfProperties);
		return decl;
	}

	private AST dfPropertyName() {
		if (attemptSkipWS("decimal-separator")) {
			return new AST(XQ.DecimalFormatPropertyDecimalSeparator);
		} else if (attemptSkipWS("grouping-separator")) {
			return new AST(XQ.DecimalFormatPropertyGroupingSeparator);
		} else if (attemptSkipWS("infinity")) {
			return new AST(XQ.DecimalFormatPropertyInfinity);
		} else if (attemptSkipWS("minus-sign")) {
			return new AST(XQ.DecimalFormatPropertyMinusSign);
		} else if (attemptSkipWS("NaN")) {
			return new AST(XQ.DecimalFormatPropertyNaN);
		} else if (attemptSkipWS("percent")) {
			return new AST(XQ.DecimalFormatPropertyPercent);
		} else if (attemptSkipWS("per-mille")) {
			return new AST(XQ.DecimalFormatPropertyPerMille);
		} else if (attemptSkipWS("zero-digit")) {
			return new AST(XQ.DecimalFormatPropertyZeroDigit);
		} else if (attemptSkipWS("digit")) {
			return new AST(XQ.DecimalFormatPropertyDigit);
		} else if (attemptSkipWS("pattern-separator")) {
			return new AST(XQ.DecimalFormatPropertyPatternSeparator);
		} else {
			return null;
		}
	}

	private AST namespaceDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "namespace");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST ncname = ncnameLiteral(false, true);
		consumeSkipWS("=");
		AST uri = uriLiteral(false, true);
		AST decl = new AST(XQ.NamespaceDeclaration);
		decl.addChild(ncname);
		decl.addChild(uri);
		return decl;
	}

	private AST importDecl() throws QueryException {
		AST importDecl = schemaImport();
		return (importDecl != null) ? importDecl : moduleImport();
	}

	private AST schemaImport() throws QueryException {
		Token la = laSkipWS("import");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "schema");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST prefix = schemaPrefix();
		AST uri = uriLiteral(false, true);
		AST[] locs = new AST[0];
		if (attemptSkipWS("at")) {
			AST locUri;
			while ((locUri = uriLiteral(true, true)) != null) {
				locs = add(locs, locUri);
			}
		}
		AST imp = new AST(XQ.SchemaImport);
		if (prefix != null) {
			imp.addChild(prefix);
		}
		imp.addChild(uri);
		imp.addChildren(locs);
		return imp;
	}

	private AST schemaPrefix() throws QueryException {
		Token la = laSkipWS("namespace");
		if (la != null) {
			consume(la);
			AST ncname = ncnameLiteral(false, true);
			consumeSkipWS("=");
			AST ns = new AST(XQ.Namespace);
			ns.addChild(ncname);
			return ns;
		}
		la = laSkipWS("default");
		consumeSkipWS("element");
		consume("namespace");
		return new AST(XQ.DefaultElementNamespace);
	}

	private AST moduleImport() throws QueryException {
		Token la = laSkipWS("import");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "module");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST prefix = null;
		Token la3 = laSkipWS("namespace");
		if (la != null) {
			consume(la3);
			AST ncname = ncnameLiteral(false, true);
			consumeSkipWS("=");
			AST ns = new AST(XQ.Namespace);
			ns.addChild(ncname);
			prefix = ns;
		}
		AST uri = uriLiteral(false, true);
		AST[] locs = new AST[0];
		if (attemptSkipWS("at")) {
			AST locUri;
			while ((locUri = uriLiteral(true, true)) != null) {
				locs = add(locs, locUri);
			}
		}
		AST imp = new AST(XQ.ModuleImport);
		if (prefix != null) {
			imp.addChild(prefix);
		}
		imp.addChild(uri);
		imp.addChildren(locs);
		return imp;
	}

	private AST contextItemDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "context");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS("item");
		AST ctxItemDecl = new AST(XQ.ContextItemDeclaration);
		if (attemptSkipWS("as")) {
			ctxItemDecl.addChild(itemType());
		}
		if (attemptSkipWS(":=")) {
			ctxItemDecl.addChild(varValue());
		} else {
			consumeSkipWS("external");
			ctxItemDecl.addChild(new AST(XQ.ExternalVariable));
			if (attemptSkipWS(":=")) {
				ctxItemDecl.addChild(varDefaultValue());
			}
		}
		return ctxItemDecl;
	}

	private AST varValue() throws QueryException {
		return exprSingle();
	}

	private AST varDefaultValue() throws QueryException {
		return exprSingle();
	}

	private AST annotatedDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		// perform look ahead
		if ((laSkipWS(la, "%") == null) && (laSkipWS(la, "variable") == null)
				&& (laSkipWS(la, "function") == null)
				// Begin XQuery Update Facility 1.0
				&& (laSkipWS(la, "updating") == null)
		// End XQuery Update Facility 1.0
		) {
			return null;
		}
		consume(la);
		AST[] anns = new AST[0];
		AST ann;
		while ((ann = annotation()) != null) {
			anns = add(anns, ann);
		}
		AST decl = varDecl();
		decl = (decl != null) ? decl : functionDecl();
		for (AST a : anns) {
			decl.insertChild(0, a);
		}
		return decl;
	}

	private AST varDecl() throws QueryException {
		if (!attemptSkipWS("variable")) {
			return null;
		}
		consumeSkipWS("$");
		String varName = declare(eqnameLiteral(false, false).getValue());
		AST varDecl = new AST(XQ.TypedVariableDeclaration);
		varDecl.addChild(new AST(XQ.Variable, varName));
		AST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			varDecl.addChild(typeDecl);
		}
		if (attemptSkipWS(":=")) {
			varDecl.addChild(varValue());
		} else {
			consumeSkipWS("external");
			varDecl.addChild(new AST(XQ.ExternalVariable));
			if (attemptSkipWS(":=")) {
				varDecl.addChild(varDefaultValue());
			}
		}
		return varDecl;
	}

	private AST functionDecl() throws QueryException {
		if (!attemptSkipWS("function")) {
			return null;
		}
		String varName = declare(eqnameLiteral(false, true).getValue());
		AST funcDecl = new AST(XQ.FunctionDecl);
		funcDecl.addChild(new AST(XQ.QNm, varName));
		consume("(");
		do {
			AST param = param();
			if (param == null) {
				break;
			}
			funcDecl.addChild(param);
		} while (attemptSkipWS(","));
		consume(")");
		if (attemptSkipWS("as")) {
			funcDecl.addChild(sequenceType());
		}
		if (attemptSkipWS("external")) {
			funcDecl.addChild(new AST(XQ.ExternalFunction));
		} else {
			funcDecl.addChild(functionBody());
		}
		return funcDecl;
	}

	private AST functionBody() throws QueryException {
		return enclosedExpr();
	}

	private AST optionDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "option");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST decl = new AST(XQ.OptionDeclaration);
		decl.addChild(eqnameLiteral(false, true));
		decl.addChild(stringLiteral(false, true));
		return decl;
	}

	private AST queryBody() throws QueryException {
		AST expr = expr();
		AST body = new AST(XQ.QueryBody);
		body.addChild(expr);
		return body;
	}

	private AST expr() throws QueryException {
		AST first = exprSingle();
		if (!attemptSkipWS(",")) {
			return first;
		}
		AST sequenceExpr = new AST(XQ.SequenceExpr);
		sequenceExpr.addChild(first);
		do {
			sequenceExpr.addChild(exprSingle());
		} while (attemptSkipWS(","));
		return sequenceExpr;
	}

	private AST exprSingle() throws QueryException {
		AST expr = flowrExpr();
		expr = (expr != null) ? expr : quantifiedExpr();
		expr = (expr != null) ? expr : switchExpr();
		expr = (expr != null) ? expr : typeswitchExpr();
		expr = (expr != null) ? expr : ifExpr();
		expr = (expr != null) ? expr : tryCatchExpr();
		// Begin XQuery Update Facility 1.0
		expr = (expr != null) ? expr : insertExpr();
		expr = (expr != null) ? expr : deleteExpr();
		expr = (expr != null) ? expr : renameExpr();
		expr = (expr != null) ? expr : replaceExpr();
		expr = (expr != null) ? expr : transformExpr();
		// End XQuery Update Facility 1.0
		expr = (expr != null) ? expr : orExpr();
		if (expr == null) {
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Non-expression faced: %s", paraphrase());
		}
		return expr;
	}

	// Begin XQuery Update Facility 1.0
	private AST insertExpr() throws QueryException {
		Token la = laSkipWS("insert");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "node");
		if (la2 == null) {
			la2 = laSkipWS(la, "nodes");
		}
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST src = exprSingle();
		AST targetChoice = insertExprTargetChoice();
		AST target = exprSingle();
		AST expr = new AST(XQ.InsertExpr);
		expr.addChild(targetChoice);
		expr.addChild(src);		
		expr.addChild(target);
		return expr;
	}

	private AST insertExprTargetChoice() throws QueryException {
		if (attemptSkipWS("as")) {
			if (attemptSkipWS("first")) {
				consumeSkipWS("into");
				return new AST(XQ.InsertFirst);
			} else if (attemptSkipWS("last")) {
				consumeSkipWS("into");
				return new AST(XQ.InsertLast);
			} else {
				throw new MismatchException("first", "last");
			}
		} else if (attemptSkipWS("into")) {
			return new AST(XQ.InsertInto);
		} else if (attemptSkipWS("after")) {
			return new AST(XQ.InsertAfter);
		} else if (attemptSkipWS("before")) {
			return new AST(XQ.InsertBefore);
		} else {
			throw new MismatchException("as", "after", "before");
		}
	}

	private AST deleteExpr() throws QueryException {
		Token la = laSkipWS("delete");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "node");
		if (la2 == null) {
			la2 = laSkipWS(la, "nodes");
		}
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST target = exprSingle();
		AST expr = new AST(XQ.DeleteExpr);
		expr.addChild(target);
		return expr;
	}

	private AST renameExpr() throws QueryException {
		Token la = laSkipWS("rename");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "node");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST target = exprSingle();
		consumeSkipWS("as");
		AST newNameExpr = exprSingle();
		AST expr = new AST(XQ.RenameExpr);
		expr.addChild(target);
		expr.addChild(newNameExpr);
		return expr;
	}

	private AST replaceExpr() throws QueryException {
		Token la = laSkipWS("replace");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "value");
		if (la2 != null) {
			Token la3 = laSkipWS(la2, "of");
			if (la3 == null) {
				return null;
			}
			Token la4 = laSkipWS(la3, "node");
			if (la4 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			consume(la3);
			consume(la4);
			AST target = exprSingle();
			AST expr = new AST(XQ.ReplaceValueExpr);
			consumeSkipWS("with");
			AST newExpr = exprSingle();
			expr.addChild(target);
			expr.addChild(newExpr);
			return expr;
		} else {
			la2 = laSkipWS(la, "node");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			AST target = exprSingle();
			AST expr = new AST(XQ.ReplaceNodeExpr);
			consumeSkipWS("with");
			AST newExpr = exprSingle();
			expr.addChild(target);
			expr.addChild(newExpr);
			return expr;
		}
	}

	private AST transformExpr() throws QueryException {
		Token la = laSkipWS("copy");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "$");
		if (la2 == null) {
			return null;
		}
		consume(la); // consume 'copy'
		AST expr = new AST(XQ.TransformExpr);
		// add all copy variables
		do {
			consumeSkipWS("$");
			AST qname = qnameLiteral(false, false);
			consumeSkipWS(":=");
			AST exprSingle = exprSingle();
			String varName = declare(qname.getValue());
			AST binding = new AST(XQ.CopyVariableBinding);
			binding.addChild(new AST(XQ.Variable, varName));
			binding.addChild(exprSingle);
			expr.addChild(binding);
		} while (attemptSkipWS(","));
		consumeSkipWS("modify");
		expr.addChild(exprSingle());
		consumeSkipWS("return");
		expr.addChild(exprSingle());
		return expr;
	}

	// End XQuery Update Facility 1.0

	private AST flowrExpr() throws QueryException {
		AST[] initialClause = initialClause();
		if ((initialClause == null) || (initialClause.length == 0)) {
			return null;
		}
		AST flworExpr = new AST(XQ.FlowrExpr);
		flworExpr.addChildren(initialClause);
		AST[] intermediateClause;
		while ((intermediateClause = intermediateClause()) != null) {
			flworExpr.addChildren(intermediateClause);
		}
		AST returnExpr = returnExpr();
		flworExpr.addChild(returnExpr);
		return flworExpr;
	}

	private AST[] initialClause() throws QueryException {
		AST[] clause = forClause();
		clause = (clause != null) ? clause : letClause();
		clause = (clause != null) ? clause : windowClause();
		return clause;
	}

	private AST[] forClause() throws QueryException {
		Token la = laSkipWS("for");
		if (la == null) {
			return null;
		}
		// la to check if var binding follows
		if (laSkipWS(la, "$") == null) {
			return null;
		}
		consume(la); // consume 'for'
		AST[] forClauses = new AST[0];
		do {
			forClauses = add(forClauses, forBinding());
		} while (attemptSkipWS(","));
		return forClauses;
	}

	private AST forBinding() throws QueryException {
		AST forClause = new AST(XQ.ForClause);
		forClause.addChild(typedVarBinding());
		if (attemptSkipWS("allowing")) {
			consumeSkipWS("empty");
			forClause.addChild(new AST(XQ.AllowingEmpty));
		}
		AST posVar = positionalVar();
		if (posVar != null) {
			forClause.addChild(posVar);
		}
		consumeSkipWS("in");
		forClause.addChild(exprSingle());
		return forClause;
	}

	private AST typedVarBinding() throws QueryException {
		if (!attemptSkipWS("$")) {
			return null;
		}
		AST eqname = eqnameLiteral(false, false);
		String varName = declare(eqname.getValue());
		AST binding = new AST(XQ.TypedVariableBinding);
		binding.addChild(new AST(XQ.Variable, varName));
		AST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			binding.addChild(typeDecl);
		}
		return binding;
	}

	private AST typeDeclaration() throws QueryException {
		if (!attemptSkipWS("as")) {
			return null;
		}
		return sequenceType();
	}

	private AST positionalVar() throws QueryException {
		if (!attemptSkipWS("at")) {
			return null;
		}
		consumeSkipWS("$");
		String varName = declare(eqnameLiteral(false, false).getValue());
		AST posVarBinding = new AST(XQ.TypedVariableBinding);
		posVarBinding.addChild(new AST(XQ.Variable, varName));
		return posVarBinding;
	}

	private String declare(String qname) {
		System.out.println("Declare " + qname);
		return qname;
	}

	private String resolve(String qname) {
		System.out.println("Resolve " + qname);
		return qname;
	}

	private AST[] letClause() throws QueryException {
		Token la = laSkipWS("let");
		if (la == null) {
			return null;
		}
		if (laSkipWS(la, "$") == null) {
			return null;
		}
		consume(la); // consume 'let'
		AST letClause = new AST(XQ.LetClause);
		letClause.addChild(typedVarBinding());
		consumeSkipWS(":=");
		letClause.addChild(exprSingle());
		return new AST[] { letClause };
	}

	private AST[] windowClause() throws QueryException {
		Token la = laSkipWS("for");
		if (la == null) {
			return null;
		}
		if (laSkipWS(la, "sliding") != null) {
			consume(la);
			return new AST[] { tumblingWindowClause() };
		}
		if (laSkipWS(la, "tumbling") != null) {
			consume(la);
			return new AST[] { slidingWindowClause() };
		}
		return null;
	}

	private AST tumblingWindowClause() throws QueryException {
		Token la = laSkipWS("sliding");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "window");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST clause = new AST(XQ.TumblingWindowClause);

		consumeSkipWS("$");
		AST eqname = eqnameLiteral(false, false);
		String varName = declare(eqname.getValue());
		AST binding = new AST(XQ.TypedVariableBinding);
		binding.addChild(new AST(XQ.Variable, varName));
		AST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			binding.addChild(typeDecl);
		}
		clause.addChild(binding);
		consumeSkipWS("in");
		clause.addChild(exprSingle());
		clause.addChild(windowStartCondition());
		if ((laSkipWS("only") != null) || (laSkipWS("end") != null)) {
			clause.addChild(windowEndCondition());
		}
		return clause;
	}

	private AST windowStartCondition() throws QueryException {
		consumeSkipWS("start");
		AST cond = new AST(XQ.WindowStartCondition);
		cond.addChildren(windowVars());
		consumeSkipWS("when");
		cond.addChild(exprSingle());
		return cond;
	}

	private AST windowEndCondition() throws QueryException {
		boolean only = attemptSkipWS("only");
		consumeSkipWS("end");
		AST cond = new AST(XQ.WindowEndCondition);
		cond.setProperty("only", Boolean.toString(only));
		cond.addChildren(windowVars());
		consumeSkipWS("when");
		cond.addChild(exprSingle());
		return cond;
	}

	private AST[] windowVars() throws QueryException {
		AST[] vars = new AST[0];
		if (attemptSkipWS("$")) {
			AST eqname = eqnameLiteral(false, false);
			String varName = declare(eqname.getValue());
			AST binding = new AST(XQ.TypedVariableBinding);
			binding.addChild(new AST(XQ.Variable, varName));
			vars = add(vars, binding);
		}
		AST posVar = positionalVar();
		if (posVar != null) {
			vars = add(vars, posVar);
		}
		if (attemptSkipWS("previous")) {
			consumeSkipWS("$");
			AST eqname = eqnameLiteral(false, false);
			String varName = declare(eqname.getValue());
			AST binding = new AST(XQ.PreviousItemBinding);
			binding.addChild(new AST(XQ.Variable, varName));
			vars = add(vars, binding);
		}
		if (attemptSkipWS("next")) {
			consumeSkipWS("$");
			AST eqname = eqnameLiteral(false, false);
			String varName = declare(eqname.getValue());
			AST binding = new AST(XQ.NextItemBinding);
			binding.addChild(new AST(XQ.Variable, varName));
			vars = add(vars, binding);
		}
		return vars;
	}

	private AST slidingWindowClause() throws QueryException {
		Token la = laSkipWS("tumbling");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "window");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST clause = new AST(XQ.SlidingWindowClause);

		consumeSkipWS("$");
		AST eqname = eqnameLiteral(false, false);
		String varName = declare(eqname.getValue());
		AST binding = new AST(XQ.TypedVariableBinding);
		binding.addChild(new AST(XQ.Variable, varName));
		AST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			binding.addChild(typeDecl);
		}
		clause.addChild(binding);
		consumeSkipWS("in");
		clause.addChild(exprSingle());
		clause.addChild(windowStartCondition());
		clause.addChild(windowEndCondition());
		return null;
	}

	private AST[] intermediateClause() throws QueryException {
		AST[] clauses = initialClause();
		if (clauses != null) {
			return clauses;
		}
		AST clause = whereClause();
		clause = (clause != null) ? clause : groupByClause();
		clause = (clause != null) ? clause : orderByClause();
		clause = (clause != null) ? clause : countClause();
		return (clause != null) ? new AST[] { clause } : null;
	}

	private AST whereClause() throws QueryException {
		if (!attemptSkipWS("where")) {
			return null;
		}
		AST whereClause = new AST(XQ.WhereClause);
		whereClause.addChild(exprSingle());
		return whereClause;
	}

	private AST groupByClause() throws QueryException {
		if (!attemptSkipWS("group")) {
			return null;
		}
		consumeSkipWS("by");
		AST groupByClause = new AST(XQ.GroupByClause);
		do {
			consumeSkipWS("$");
			AST gs = new AST(XQ.GroupBySpec);
			String varName = resolve(eqnameLiteral(false, false).getValue());
			gs.addChild(new AST(XQ.VariableRef, varName));
			if (attemptSkipWS("collation")) {
				AST uriLiteral = uriLiteral(false, true);
				AST collation = new AST(XQ.Collation);
				collation.addChild(uriLiteral);
				gs.addChild(collation);
			}
		} while (attemptSkipWS(","));
		return groupByClause;
	}

	private AST orderByClause() throws QueryException {
		if (attemptSkipWS("stable")) {
			consumeSkipWS("order");
		} else if (!attemptSkipWS("order")) {
			return null;
		}
		consumeSkipWS("by");
		AST clause = new AST(XQ.OrderByClause);
		do {
			AST os = new AST(XQ.OrderBySpec);
			clause.addChild(os);
			os.addChild(exprSingle());			
			if (attemptSkipWS("ascending")) {
				AST obk = new AST(XQ.OrderByKind);
				obk.addChild(new AST(XQ.ASCENDING));
				os.addChild(obk);
			} else if (attemptSkipWS("descending")) {
				AST obk = new AST(XQ.OrderByKind);
				obk.addChild(new AST(XQ.DESCENDING));
				os.addChild(obk);
			}
			if (attemptSkipWS("empty")) {
				if (attemptSkipWS("greatest")) {
					AST obem = new AST(XQ.OrderByEmptyMode);
					obem.addChild(new AST(XQ.GREATEST));
					os.addChild(obem);
				} else if (attemptSkipWS("least")) {
					AST obem = new AST(XQ.OrderByEmptyMode);
					obem.addChild(new AST(XQ.LEAST));
					os.addChild(obem);
				}
			}
			if (attemptSkipWS("collation")) {
				AST uriLiteral = uriLiteral(false, true);
				AST collation = new AST(XQ.Collation);
				collation.addChild(uriLiteral);
				os.addChild(collation);
			}			
		} while (attemptSkipWS(","));
		return clause;
	}

	private AST countClause() throws QueryException {
		Token la = laSkipWS("count");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "$");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		String varName = declare(eqnameLiteral(false, false).getValue());
		AST countClause = new AST(XQ.CountClause);
		countClause.addChild(new AST(XQ.Variable, varName));
		return countClause;
	}

	private AST returnExpr() throws QueryException {
		if (!attemptSkipWS("return")) {
			return null;
		}
		AST returnExpr = new AST(XQ.ReturnClause);
		returnExpr.addChild(exprSingle());
		return returnExpr;
	}

	private AST quantifiedExpr() throws QueryException {
		AST quantifier;
		if (attemptSkipWS("some")) {
			quantifier = new AST(XQ.SomeQuantifier);
		} else if (attemptSkipWS("every")) {
			quantifier = new AST(XQ.EveryQuantifier);
		} else {
			return null;
		}
		// la to check if var binding follows
		if (laSkipWS("$") == null) {
			return null;
		}
		AST qExpr = new AST(XQ.QuantifiedExpr);
		qExpr.addChild(quantifier);
		qExpr.addChild(typedVarBinding());
		consumeSkipWS("in");
		qExpr.addChild(exprSingle());
		while (attemptSkipWS(",")) {
			AST binding = typedVarBinding();
			if (binding == null) {
				throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
						"Expected variable binding: %s", paraphrase());
			}
			qExpr.addChild(binding);
			consumeSkipWS("in");
			qExpr.addChild(exprSingle());
		}
		consumeSkipWS("satisfies");
		qExpr.addChild(exprSingle());
		return qExpr;
	}

	private AST switchExpr() throws QueryException {
		Token la = laSkipWS("switch");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST sExpr = new AST(XQ.SwitchExpr);
		sExpr.addChild(expr());
		consumeSkipWS(")");
		AST clause = switchClause();
		if (clause == null) {
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Excpected switch clause: %s", paraphrase());
		}
		sExpr.addChild(clause);
		while ((clause = switchClause()) != null) {
			sExpr.addChild(clause);
		}
		consumeSkipWS("default");
		consumeSkipWS("return");
		sExpr.addChild(exprSingle());
		return sExpr;
	}

	private AST switchClause() throws QueryException {
		if (!attemptSkipWS("case")) {
			return null;
		}
		AST clause = new AST(XQ.SwitchClause);
		clause.addChild(exprSingle());
		consumeSkipWS("return");
		clause.addChild(exprSingle());
		return clause;
	}

	private AST typeswitchExpr() throws QueryException {
		Token la = laSkipWS("typeswitch");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST tsExpr = new AST(XQ.TypeSwitch);
		tsExpr.addChild(expr());
		consumeSkipWS(")");
		AST clause = caseClause();
		if (clause == null) {
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Excpected case clause: %s", paraphrase());
		}
		tsExpr.addChild(clause);
		while ((clause = caseClause()) != null) {
			tsExpr.addChild(clause);
		}
		consumeSkipWS("default");
		AST dftClause = new AST(XQ.TypeSwitchCase);
		if (attemptSkipWS("$")) {
			AST eqname = eqnameLiteral(false, false);
			String varName = declare(eqname.getValue());
			dftClause.addChild(new AST(XQ.Variable, varName));
		}
		consumeSkipWS("return");
		dftClause.addChild(exprSingle());
		tsExpr.addChild(dftClause);
		return tsExpr;
	}

	private AST caseClause() throws QueryException {
		if (!attemptSkipWS("case")) {
			return null;
		}
		AST clause = new AST(XQ.TypeSwitchCase);
		if (attemptSkipWS("$")) {
			AST eqname = eqnameLiteral(false, false);
			String varName = declare(eqname.getValue());
			clause.addChild(new AST(XQ.Variable, varName));
			consumeSkipWS("as");
		}
		do {
			clause.addChild(sequenceType());
		} while (attemptSkipWS("|"));
		consumeSkipWS("return");
		clause.addChild(exprSingle());
		return clause;
	}

	private AST ifExpr() throws QueryException {
		Token la = laSkipWS("if");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST ifExpr = new AST(XQ.IfExpr);
		ifExpr.addChild(exprSingle());
		consumeSkipWS(")");
		consumeSkipWS("then");
		ifExpr.addChild(exprSingle());
		consumeSkipWS("else");
		ifExpr.addChild(exprSingle());
		return ifExpr;
	}

	private AST tryCatchExpr() throws QueryException {
		Token la = laSkipWS("try");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "{");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST tcExpr = new AST(XQ.TryCatchExpr);
		tcExpr.addChild(expr());
		consumeSkipWS("}");
		AST clause = tryClause();
		if (clause == null) {
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Excpected try clause: %s", paraphrase());
		}
		tcExpr.addChild(clause);
		while ((clause = tryClause()) != null) {
			tcExpr.addChild(clause);
		}
		return tcExpr;
	}

	private AST tryClause() throws QueryException {
		if (!attemptSkipWS("catch")) {
			return null;
		}
		AST clause = new AST(XQ.CatchClause);
		clause.addChild(catchErrorList());
		clause.addChild(catchVars());
		consumeSkipWS("{");
		clause.addChild(expr());
		consumeSkipWS("}");
		return clause;
	}

	private AST catchErrorList() throws QueryException {
		AST list = new AST(XQ.CatchErrorList);
		do {
			list.addChild(nameTest());
		} while (attemptSkipWS("|"));
		return list;
	}

	private AST catchVars() throws QueryException {
		consumeSkipWS("(");
		AST vars = new AST(XQ.CatchVar);
		consumeSkipWS("$");
		vars.addChild(eqnameLiteral(false, false));
		if (attemptSkipWS(",")) {
			consumeSkipWS("$");
			vars.addChild(eqnameLiteral(false, false));
			if (attemptSkipWS(",")) {
				consumeSkipWS("$");
				vars.addChild(eqnameLiteral(false, false));
			}
		}
		consumeSkipWS(")");
		return vars;
	}

	private AST orExpr() throws QueryException {
		AST first = andExpr();
		// additional space used to disambiguate
		// 'expr() or expr()' and '... expr() order by...' 
		if (!attemptSkipWS("or ")) {
			return first;
		}
		AST second = andExpr();
		AST expr = new AST(XQ.OrExpr);
		expr.addChild(first);
		expr.addChild(second);
		return expr;
	}

	private AST andExpr() throws QueryException {
		AST first = comparisonExpr();
		if (!attemptSkipWS("and")) {
			return first;
		}
		AST second = comparisonExpr();
		AST expr = new AST(XQ.AndExpr);
		expr.addChild(first);
		expr.addChild(second);
		return expr;
	}

	private AST comparisonExpr() throws QueryException {
		AST first = rangeExpr();
		AST cmp;
		// additional space used for disambiguation
		if (attemptSkipWS("=")) {
			cmp = new AST(XQ.GeneralCompEQ);
		} else if (attemptSkipWS("!=")) {
			cmp = new AST(XQ.GeneralCompNE);
		} else if (attemptSkipWS("<")) {
			cmp = new AST(XQ.GeneralCompLT);
		} else if (attemptSkipWS("<=")) {
			cmp = new AST(XQ.GeneralCompLE);
		} else if (attemptSkipWS(">")) {
			cmp = new AST(XQ.GeneralCompGT);
		} else if (attemptSkipWS(">=")) {
			cmp = new AST(XQ.GeneralCompGE);
		} else if (attemptSkipWS("eq")) {
			cmp = new AST(XQ.ValueCompEQ);
		} else if (attemptSkipWS("neq")) {
			cmp = new AST(XQ.ValueCompNE);
		} else if (attemptSkipWS("lt")) {
			cmp = new AST(XQ.ValueCompLT);
		} else if (attemptSkipWS("le ")) {
			// additional space used to disambiguate
			// 'expr() le expr()' and '... expr() let $...'
			cmp = new AST(XQ.ValueCompLE);
		} else if (attemptSkipWS("gt")) {
			cmp = new AST(XQ.ValueCompGT);
		} else if (attemptSkipWS("ge")) {
			cmp = new AST(XQ.ValueCompGE);
		} else if (attemptSkipWS("is")) {
			cmp = new AST(XQ.NodeCompIs);
		} else if (attemptSkipWS("<<")) {
			cmp = new AST(XQ.NodeCompPrecedes);
		} else if (attemptSkipWS(">>")) {
			cmp = new AST(XQ.NodeCompFollows);
		} else {
			return first;
		}
		AST second = comparisonExpr();
		AST expr = new AST(XQ.ComparisonExpr);
		expr.addChild(cmp);
		expr.addChild(first);		
		expr.addChild(second);
		return expr;
	}

	private AST rangeExpr() throws QueryException {
		AST first = additiveExpr();
		if (!attemptSkipWS("to")) {
			return first;
		}
		AST second = additiveExpr();
		AST expr = new AST(XQ.RangeExpr);
		expr.addChild(first);
		expr.addChild(second);
		return expr;
	}

	private AST additiveExpr() throws QueryException {
		AST first = multiplicativeExpr();
		while (true) {
			AST op;
			if (attemptSkipWS("+")) {
				op = new AST(XQ.AddOp);
			} else if (attemptSkipWS("-")) {
				op = new AST(XQ.SubtractOp);
			} else {
				return first;
			}
			AST second = multiplicativeExpr();
			AST expr = new AST(XQ.ArithmeticExpr);
			expr.addChild(op);
			expr.addChild(first);
			expr.addChild(second);
			first = expr;
		}
	}

	private AST multiplicativeExpr() throws QueryException {
		AST first = unionExpr();
		while (true) {
			AST op;
			if (attemptSkipWS("*")) {
				op = new AST(XQ.MultiplyOp);
			} else if (attemptSkipWS("div")) {
				op = new AST(XQ.DivideOp);
			} else if (attemptSkipWS("idiv")) {
				op = new AST(XQ.IDivideOp);
			} else if (attemptSkipWS("mod ")) {
				// additional space used to disambiguate
				op = new AST(XQ.ModulusOp);
			} else {
				return first;
			}
			AST second = unionExpr();
			AST expr = new AST(XQ.ArithmeticExpr);
			expr.addChild(op);
			expr.addChild(first);
			expr.addChild(second);
			first = expr;
		}
	}

	private AST unionExpr() throws QueryException {
		AST first = intersectExpr();
		if ((!attemptSkipWS("union")) && (!attemptSkipWS("|"))) {
			return first;
		}
		AST second = intersectExpr();
		AST expr = new AST(XQ.UnionExpr);
		expr.addChild(first);
		expr.addChild(second);
		return expr;
	}

	private AST intersectExpr() throws QueryException {
		AST first = instanceOfExpr();
		AST expr;
		if (attemptSkipWS("intersect")) {
			expr = new AST(XQ.IntersectExpr);
		} else if (attemptSkipWS("except")) {
			expr = new AST(XQ.ExceptExpr);
		} else {
			return first;
		}
		AST second = instanceOfExpr();
		expr.addChild(first);
		expr.addChild(second);
		return expr;
	}

	private AST instanceOfExpr() throws QueryException {
		AST first = treatExpr();
		if (!attemptSkipWS("instance")) {
			return first;
		}
		consumeSkipWS("of");
		AST type = sequenceType();
		AST expr = new AST(XQ.InstanceofExpr);
		expr.addChild(first);
		expr.addChild(type);
		return expr;
	}

	private AST sequenceType() throws QueryException {
		AST type = emptySequence();
		AST occInd = null;
		if (type == null) {
			type = itemType();
			occInd = occurrenceIndicator();
		}
		AST typeDecl = new AST(XQ.SequenceType);
		typeDecl.addChild(type);
		if (occInd != null) {
			typeDecl.addChild(occInd);
		}
		return typeDecl;
	}

	private AST emptySequence() throws QueryException {
		Token la = laSkipWS("empty-sequence");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(")");
		return new AST(XQ.EmptySequenceType);
	}

	private AST anyKind() throws QueryException {
		Token la = laSkipWS("item");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(")");
		return new AST(XQ.ItemType);
	}

	private AST occurrenceIndicator() {
		if (attemptSkipWS("?")) {
			return new AST(XQ.CardinalityZeroOrOne);
		}
		if (attemptSkipWS("*")) {
			return new AST(XQ.CardinalityZeroOrMany);
		}
		if (attemptSkipWS("+")) {
			return new AST(XQ.CardinalityOneOrMany);
		}
		return null;
	}

	private AST itemType() throws QueryException {
		AST type = kindTest();
		type = (type != null) ? type : anyKind();
		type = (type != null) ? type : functionTest();
		type = (type != null) ? type : atomicOrUnionType();
		type = (type != null) ? type : parenthesizedItemType();
		return type;
	}

	private AST functionTest() throws QueryException {
		AST funcTest = null;
		AST ann;
		while ((ann = annotation()) != null) {
			if (funcTest == null) {
				funcTest = new AST(XQ.FunctionTest);
			}
			funcTest.addChild(ann);
		}
		AST test = anyFunctionTest();
		test = (test != null) ? test : typedFunctionTest();
		if (test == null) {
			if (funcTest != null) {
				throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
						"Expected function test: %s", paraphrase());
			}
			return null;
		}
		if (funcTest == null) {
			funcTest = new AST(XQ.FunctionTest);
		}
		funcTest.addChild(test);
		return funcTest;
	}

	private AST annotation() throws QueryException {
		// Begin XQuery Update Facility 1.0
		// treat old-school updating keyword as special "annotation"
		if (attemptSkipWS("updating")) {
			return new AST(XQ.Annotation, "updating");
		}
		// End XQuery Update Facility 1.0
		if (!attemptSkipWS("%")) {
			return null;
		}
		AST name = eqnameLiteral(false, true);
		AST ann = new AST(XQ.Annotation, name.getValue());
		if (attemptSkipWS("(")) {
			do {
				ann.addChild(stringLiteral(false, true));
			} while (attemptSkipWS(","));
			consumeSkipWS(")");
		}
		return ann;
	}

	private AST anyFunctionTest() throws QueryException {
		Token la = laSkipWS("namespace-node");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		Token la3 = laSkipWS(la2, "*");
		if (la3 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consume(la3);
		consumeSkipWS(")");
		return new AST(XQ.AnyFunctionType);
	}

	private AST typedFunctionTest() throws QueryException {
		Token la = laSkipWS("namespace-node");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST typedFunc = new AST(XQ.TypedFunctionType);
		if (!attemptSkipWS(")")) {
			do {
				typedFunc.addChild(sequenceType());
			} while (attemptSkipWS(","));
			consumeSkipWS(")");
		}
		consumeSkipWS("as");
		typedFunc.addChild(sequenceType());
		return typedFunc;
	}

	private AST atomicOrUnionType() throws QueryException {
		AST eqname = eqnameLiteral(true, true);
		AST aouType = new AST(XQ.AtomicOrUnionType);
		aouType.addChild(eqname);
		return aouType;
	}

	private AST parenthesizedItemType() throws QueryException {
		if (!attemptSkipWS("(")) {
			return null;
		}
		AST itemType = itemType();
		consumeSkipWS(")");
		return itemType;
	}

	private AST singleType() throws QueryException {
		AST aouType = atomicOrUnionType();
		if (aouType == null) {
			return null;
		}
		AST type = new AST(XQ.SequenceType);
		type.addChild(aouType);
		if (attemptSkipWS("?")) {
			type.addChild(new AST(XQ.CardinalityZeroOrOne));
		}
		return type;
	}

	private AST treatExpr() throws QueryException {
		AST first = castableExpr();
		if (!attemptSkipWS("treat")) {
			return first;
		}
		consumeSkipWS("as");
		AST type = sequenceType();
		AST expr = new AST(XQ.TreatExpr);
		expr.addChild(first);
		expr.addChild(type);
		return expr;
	}

	private AST castableExpr() throws QueryException {
		AST first = castExpr();
		if (!attemptSkipWS("castable")) {
			return first;
		}
		consumeSkipWS("as");
		AST type = singleType();
		AST expr = new AST(XQ.CastableExpr);
		expr.addChild(first);
		expr.addChild(type);
		return expr;
	}

	private AST castExpr() throws QueryException {
		AST first = unaryExpr();
		// additional space used to disambiguate
		// 'expr() cast as...' and '... expr() castable ...'
		if (!attemptSkipWS("cast ")) {
			return first;
		}
		consumeSkipWS("as");
		AST type = singleType();
		AST expr = new AST(XQ.CastExpr);
		expr.addChild(first);
		expr.addChild(type);
		return expr;
	}

	private AST unaryExpr() throws QueryException {
		int minusCount = 0;
		while (true) {
			if (attemptSkipWS("+")) {
				continue;
			}
			if (attemptSkipWS("-")) {
				minusCount++;
				continue;
			}
			break;
		}
		if ((minusCount & 1) == 0) {
			return valueExpr();
		}
		AST expr = new AST(XQ.ArithmeticExpr);
		expr.addChild(new AST(XQ.Int, "-1"));
		expr.addChild(new AST(XQ.MultiplyOp));
		expr.addChild(valueExpr());
		return expr;
	}

	private AST valueExpr() throws QueryException {
		AST expr = validateExpr();
		expr = (expr != null) ? expr : pathExpr();
		expr = (expr != null) ? expr : extensionExpr();
		return expr;
	}

	private AST extensionExpr() throws QueryException {
		AST pragma = pragma();
		if (pragma == null) {
			return null;
		}
		AST eExpr = new AST(XQ.ExtensionExpr);
		eExpr.addChild(pragma);
		while ((pragma = pragma()) != null) {
			eExpr.addChild(pragma);
		}
		consumeSkipWS("{");
		if (!attemptSkipWS("}")) {
			eExpr.addChild(expr());
			consumeSkipWS("}");
		}
		return eExpr;
	}

	private AST pragma() throws QueryException {
		if (!attemptSkipWS("(#")) {
			return null;
		}
		AST pragma = new AST(XQ.Pragma);
		attemptWS();
		pragma.addChild(qnameLiteral(false, false));
		if (!attemptWS()) {
			pragma.addChild(pragmaContent());
		}
		consume("#)");
		return pragma;
	}

	private AST pathExpr() throws QueryException {
		// treatment of initial '/' and '//' is
		// delayed to relativePathExpr
		return relativePathExpr();
	}

	private AST relativePathExpr() throws QueryException {
		AST[] path = null;
		AST step;
		if (attemptSkipWS("//")) {
			step = stepExpr();
			if (step == null) {
				throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
						"Incomplete path step: %s", paraphrase());
			}
			// initial '//' is translated to
			// fn:root(self::node()) treat as
			// document-node())/descendant-or-self::node()/
			AST treat = fnRootTreatAsDocument();
			AST dosn = descendantOrSelfNode();
			path = new AST[] { treat, dosn, step };
		} else if (attemptSkipWS("/")) {
			step = stepExpr();
			if (step == null) {
				// leading-lone-slash rule:
				// single '/' is translated to
				// (fn:root(self::node()) treat as document-node())
				return fnRootTreatAsDocument();
			}
			// initial '/' is translated to
			// (fn:root(self::node()) treat as document-node())/
			AST treat = fnRootTreatAsDocument();
			path = new AST[] { treat, step };
		} else {
			step = stepExpr();
			if (step == null) {
				return null;
			}
			path = new AST[] { step };
		}

		while (true) {
			if (attemptSkipWS("//")) {
				// intermediate '//' is translated to
				// descendant-or-self::node()/
				path = add(path, descendantOrSelfNode());
			} else if (!attemptSkipWS("/")) {
				break;
			}
			step = stepExpr();
			if (step == null) {
				throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
						"Incomplete path step: %s", paraphrase());
			}
			path = add(path, step);
		}
		if (path.length == 1) {
			return path[0];
		}
		AST pathExpr = new AST(XQ.PathExpr);
		pathExpr.addChildren(path);
		return pathExpr;
	}

	private AST descendantOrSelfNode() {
		AST dosn = new AST(XQ.StepExpr);
		AST axisSpec = new AST(XQ.AxisSpec);
		axisSpec.addChild(new AST(XQ.DESCENDANT_OR_SELF));
		dosn.addChild(axisSpec);
		dosn.addChild(new AST(XQ.KindTestAnyKind));
		return dosn;
	}

	private AST fnRootTreatAsDocument() {
		AST treat = new AST(XQ.TreatExpr);
		AST call = new AST(XQ.FunctionCall, "fn:root");
		AST step = new AST(XQ.StepExpr);
		AST axisSpec = new AST(XQ.AxisSpec);
		axisSpec.addChild(new AST(XQ.SELF));
		step.addChild(axisSpec);
		step.addChild(new AST(XQ.KindTestAnyKind));
		AST seqType = new AST(XQ.SequenceType);
		seqType.addChild(new AST(XQ.KindTestDocument));
		call.addChild(step);
		treat.addChild(call);
		treat.addChild(seqType);
		return treat;
	}

	private AST stepExpr() throws QueryException {
		AST expr = postFixExpr();
		if (expr != null) {
			return expr;
		}
		return axisStep();
	}

	private AST axisStep() throws QueryException {
		AST[] step = forwardStep();
		if (step == null) {
			step = reverseStep();
		}
		if (step == null) {
			return null;
		}
		AST[] predicateList = predicateList();
		AST stepExpr = new AST(XQ.StepExpr);
		stepExpr.addChildren(step);
		if (predicateList != null) {
			stepExpr.addChildren(predicateList);
		}
		return stepExpr;
	}

	private AST[] forwardStep() throws QueryException {
		AST forwardAxis = forwardAxis();
		if (forwardAxis == null) {
			return abbrevForwardStep();
		}
		AST axisSpec = new AST(XQ.AxisSpec);
		axisSpec.addChild(forwardAxis);
		return new AST[] { axisSpec, nodeTest() };
	}

	private AST forwardAxis() {
		Token la;
		AST axis;
		if ((la = laSkipWS("child")) != null) {
			axis = new AST(XQ.CHILD);
		} else if ((la = laSkipWS("descendant")) != null) {
			axis = new AST(XQ.DESCENDANT);
		} else if ((la = laSkipWS("attribute")) != null) {
			axis = new AST(XQ.ATTRIBUTE);
		} else if ((la = laSkipWS("self")) != null) {
			axis = new AST(XQ.SELF);
		} else if ((la = laSkipWS("descendant-or-self")) != null) {
			axis = new AST(XQ.DESCENDANT_OR_SELF);
		} else if ((la = laSkipWS("following-sibling")) != null) {
			axis = new AST(XQ.FOLLOWING_SIBLING);
		} else if ((la = laSkipWS("following")) != null) {
			axis = new AST(XQ.FOLLOWING);
		} else {
			return null;
		}
		Token la2 = laSkipWS(la, "::");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		return axis;
	}

	private AST[] abbrevForwardStep() throws QueryException {
		boolean attributeAxis = false;
		if (attemptSkipWS("@")) {
			attributeAxis = true;
		} else {
			// look ahead if node test will
			// be attribute or schema-attribute test
			Token la = laSkipWS("attribute");
			if (la == null) {
				la = laSkipWS("schema-attribute");
			}
			if ((la != null) && (laSkipWS(la, "(") != null)) {
				attributeAxis = true;
			}
		}
		AST nodeTest = nodeTest();
		if (nodeTest == null) {
			return null;
		}
		if (attributeAxis) {
			AST axisSpec = new AST(XQ.AxisSpec);
			axisSpec.addChild(new AST(XQ.ATTRIBUTE));
			return new AST[] { axisSpec, nodeTest };
		} else {
			AST axisSpec = new AST(XQ.AxisSpec);
			axisSpec.addChild(new AST(XQ.CHILD));
			return new AST[] { axisSpec, nodeTest };
		}
	}

	private AST[] reverseStep() throws QueryException {
		AST forwardAxis = reverseAxis();
		if (forwardAxis == null) {
			return abbrevReverseStep();
		}
		AST axisSpec = new AST(XQ.AxisSpec);
		axisSpec.addChild(forwardAxis);
		return new AST[] { axisSpec, nodeTest() };
	}

	private AST reverseAxis() {
		Token la;
		AST axis;
		if ((la = laSkipWS("parent")) != null) {
			axis = new AST(XQ.PARENT);
		} else if ((la = laSkipWS("ancestor")) != null) {
			axis = new AST(XQ.ANCESTOR);
		} else if ((la = laSkipWS("preceding-sibling")) != null) {
			axis = new AST(XQ.PRECEDING_SIBLING);
		} else if ((la = laSkipWS("preceding")) != null) {
			axis = new AST(XQ.PRECEDING);
		} else if ((la = laSkipWS("ancestor-or-self")) != null) {
			axis = new AST(XQ.ANCESTOR_OR_SELF);
		} else {
			return null;
		}
		Token la2 = laSkipWS(la, "::");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		return axis;
	}

	private AST[] abbrevReverseStep() {
		if (!attemptSkipWS("..")) {
			return null;
		}
		AST axisSpec = new AST(XQ.AxisSpec);
		axisSpec.addChild(new AST(XQ.PARENT));
		AST nameTest = new AST(XQ.NameTest);
		nameTest.addChild(new AST(XQ.Wildcard));
		return new AST[] { axisSpec, nameTest };
	}

	private AST nodeTest() throws QueryException {
		AST test = kindTest();
		test = (test != null) ? test : nameTest();
		return test;
	}

	private AST kindTest() throws QueryException {
		AST test = documentTest();
		test = (test != null) ? test : elementTest();
		test = (test != null) ? test : attributeTest();
		test = (test != null) ? test : schemaElementTest();
		test = (test != null) ? test : schemaAttributeTest();
		test = (test != null) ? test : piTest();
		test = (test != null) ? test : commentTest();
		test = (test != null) ? test : textTest();
		test = (test != null) ? test : namespaceNodeTest();
		test = (test != null) ? test : anyKindTest();
		return test;
	}

	private AST documentTest() throws QueryException {
		Token la = laSkipWS("document-node");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST elTest = elementTest();
		AST schemaElTest = (elTest != null) ? elTest : schemaElementTest();
		consumeSkipWS(")");
		AST docTest = new AST(XQ.KindTestDocument);
		if (elTest != null) {
			docTest.addChild(elTest);
		}
		if (schemaElTest != null) {
			docTest.addChild(schemaElTest);
		}
		return docTest;
	}

	private AST elementTest() throws QueryException {
		Token la = laSkipWS("element");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST enow = elementNameOrWildcard();
		AST tn = null;
		AST nilled = null;
		if ((enow != null) && (attemptSkipWS(","))) {
			tn = eqnameLiteral(true, true);
			if (attemptSkipWS("?")) {
				nilled = new AST(XQ.Nilled);
			}
		}
		consumeSkipWS(")");
		AST elTest = new AST(XQ.KindTestElement);
		if (enow != null) {
			elTest.addChild(enow);
		}
		if (tn != null) {
			elTest.addChild(tn);
		}
		if (nilled != null) {
			elTest.addChild(nilled);
		}
		return elTest;
	}

	private AST attributeTest() throws QueryException {
		Token la = laSkipWS("attribute");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST anow = attributeNameOrWildcard();
		AST tn = null;
		AST nilled = null;
		if ((anow != null) && (attemptSkipWS(","))) {
			tn = eqnameLiteral(true, true);
			if (attemptSkipWS("?")) {
				nilled = new AST(XQ.Nilled);
			}
		}
		consumeSkipWS(")");
		AST attTest = new AST(XQ.KindTestAttribute);
		if (anow != null) {
			attTest.addChild(anow);
		}
		if (tn != null) {
			attTest.addChild(tn);
		}
		if (nilled != null) {
			attTest.addChild(nilled);
		}
		return attTest;
	}

	private AST schemaElementTest() throws QueryException {
		Token la = laSkipWS("schema-element");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST name = eqnameLiteral(false, true);
		consumeSkipWS(")");
		AST test = new AST(XQ.KindTestSchemaElement);
		test.addChild(name);
		return test;
	}

	private AST schemaAttributeTest() throws QueryException {
		Token la = laSkipWS("schema-attribute");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST name = eqnameLiteral(false, true);
		consumeSkipWS(")");
		AST test = new AST(XQ.KindTestSchemaAttribute);
		test.addChild(name);
		return test;
	}

	private AST piTest() throws QueryException {
		Token la = laSkipWS("processing-instruction");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST name = ncnameLiteral(true, true);
		name = (name != null) ? name : stringLiteral(false, true);
		consumeSkipWS(")");
		AST test = new AST(XQ.KindTestPi);
		test.addChild(name);
		return test;
	}

	private AST commentTest() throws QueryException {
		Token la = laSkipWS("comment");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(")");
		return new AST(XQ.KindTestComment);
	}

	private AST textTest() throws QueryException {
		Token la = laSkipWS("text");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(")");
		return new AST(XQ.KindTestText);
	}

	private AST namespaceNodeTest() throws QueryException {
		Token la = laSkipWS("namespace-node");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(")");
		return new AST(XQ.KindTestNamespaceNode);
	}

	private AST anyKindTest() throws QueryException {
		Token la = laSkipWS("node");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(")");
		return new AST(XQ.KindTestAnyKind);
	}

	private AST elementNameOrWildcard() throws QueryException {
		AST enow = eqnameLiteral(true, true);
		if (enow != null) {
			return enow;
		}
		if (attemptSkipWS("*")) {
			return new AST(XQ.Wildcard);
		}
		return null;
	}

	private AST attributeNameOrWildcard() throws QueryException {
		AST anow = eqnameLiteral(true, true);
		if (anow != null) {
			return anow;
		}
		if (attemptSkipWS("*")) {
			return new AST(XQ.Wildcard);
		}
		return null;
	}

	private AST nameTest() throws QueryException {
		AST test = eqnameLiteral(true, true);
		test = (test != null) ? test : wildcard();
		if (test == null) {
			return null;
		}
		AST nameTest = new AST(XQ.NameTest);
		nameTest.addChild(test);
		return nameTest;
	}

	private AST wildcard() throws QueryException {
		if (attemptSkipWS("*:")) {
			AST ncname = ncnameLiteral(true, true);
			if (ncname == null) {
				return null;
			}
			AST wbc = new AST(XQ.WildcardBeforeColon);
			wbc.addChild(ncname);
			return wbc;
		} else if (attemptSkipWS("*")) {
			return new AST(XQ.Wildcard);
		} else {
			AST ncname = ncnameLiteral(true, true);
			if (ncname == null) {
				return null;
			}
			if (!attempt(":*")) {
				return null;
			}
			AST wba = new AST(XQ.WildcardAfterColon);
			wba.addChild(ncname);
			return wba;
		}
	}

	private AST[] predicateList() throws QueryException {
		AST[] predicates = new AST[0];
		AST predicate;
		while ((predicate = predicate()) != null) {
			predicates = add(predicates, predicate);
		}
		return predicates;
	}

	private AST predicate() throws QueryException {
		if (!attemptSkipWS("[")) {
			return null;
		}
		AST pred = new AST(XQ.Predicate);
		pred.addChild(expr());
		consume("]");
		return pred;

	}

	private AST validateExpr() throws QueryException {
		if (!attemptSkipWS("validate")) {
			return null;
		}
		AST vExpr = new AST(XQ.ValidateExpr);
		if (attemptSkipWS("lax")) {
			vExpr.addChild(new AST(XQ.ValidateLax));
		} else if (attemptSkipWS("strict")) {
			vExpr.addChild(new AST(XQ.ValidateStrict));
		} else if (attemptSkipWS("type")) {
			vExpr.addChild(eqnameLiteral(false, true));
		} else {
			throw new MismatchException("lax", "strict", "type");
		}
		consumeSkipWS("{");
		vExpr.addChild(expr());
		consumeSkipWS("}");
		return vExpr;
	}

	private AST postFixExpr() throws QueryException {
		AST expr = primaryExpr();
		while (true) {
			AST predicate = predicate();
			if (predicate != null) {
				AST filterExpr = new AST(XQ.FilterExpr);
				filterExpr.addChild(expr);
				filterExpr.addChild(predicate);
				expr = filterExpr;
				continue;
			}
			AST[] argumentList = argumentList();
			if ((argumentList != null) && (argumentList.length > 0)) {
				AST dynFuncCallExpr = new AST(XQ.DynamicFunctionCallExpr);
				dynFuncCallExpr.addChild(expr);
				dynFuncCallExpr.addChildren(argumentList);
				expr = dynFuncCallExpr;
				continue;
			}
			break;
		}
		return expr;
	}

	private AST[] argumentList() throws QueryException {
		if (!attemptSkipWS("(")) {
			return null;
		}
		AST[] args = new AST[0];
		while (!attemptSkipWS(")")) {
			if (args.length > 0) {
				consumeSkipWS(",");
			}
			args = add(args, argument());
		}
		return args;
	}

	private AST primaryExpr() throws QueryException {
		AST expr = literal();
		expr = (expr != null) ? expr : varRef();
		expr = (expr != null) ? expr : parenthesizedExpr();
		expr = (expr != null) ? expr : contextItemExpr();
		expr = (expr != null) ? expr : functionCall();
		expr = (expr != null) ? expr : orderedExpr();
		expr = (expr != null) ? expr : unorderedExpr();
		expr = (expr != null) ? expr : constructor();
		expr = (expr != null) ? expr : functionItemExpr();
		return expr;
	}

	private AST literal() throws QueryException {
		AST lit = numericLiteral();
		lit = (lit != null) ? lit : stringLiteral(true, true);
		return lit;
	}

	private AST varRef() throws QueryException {
		if (!attemptSkipWS("$")) {
			return null;
		}
		return new AST(XQ.VariableRef, resolve(eqnameLiteral(false, false)
				.getValue()));
	}

	private AST parenthesizedExpr() throws QueryException {
		if (!attemptSkipWS("(")) {
			return null;
		}
		if (attemptSkipWS(")")) {
			return new AST(XQ.EmptySequence);
		}
		AST expr = expr();
		consumeSkipWS(")");
		return expr;
	}

	private AST contextItemExpr() {
		if (!attemptSkipWS(".")) {
			return null;
		}
		return new AST(XQ.ContextItemExpr);
	}

	private AST functionCall() throws QueryException {
		Token la;
		try {
			la = laEQNameSkipWS(true);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			return null;
		}
		String funcName = la.string();
		if (isReservedFuncName(funcName)) {
			return null;
		}
		if (laSkipWS(la, "(") == null) {
			return null;
		}
		consume(la);
		AST call = new AST(XQ.FunctionCall, funcName);
		call.addChildren(argumentList());
		return call;
	}

	private AST argument() throws QueryException {
		// changed order to match '?' greedy
		if (attempt("?")) {
			return new AST(XQ.ArgumentPlaceHolder);
		}
		return exprSingle();
	}

	private boolean isReservedFuncName(String string) {
		for (String fun : RESERVED_FUNC_NAMES) {
			if (fun.equals(string)) {
				return true;
			}
		}
		return false;
	}

	private AST orderedExpr() throws QueryException {
		Token la = laSkipWS("ordered");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "{");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST expr = expr();
		consumeSkipWS("}");
		AST orderedExpr = new AST(XQ.OrderedExpr);
		orderedExpr.addChild(expr);
		return orderedExpr;
	}

	private AST unorderedExpr() throws QueryException {
		Token la = laSkipWS("unordered");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "{");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST expr = expr();
		consumeSkipWS("}");
		AST unorderedExpr = new AST(XQ.UnorderedExpr);
		unorderedExpr.addChild(expr);
		return unorderedExpr;
	}

	private AST constructor() throws QueryException {
		AST con = directConstructor();
		con = (con != null) ? con : computedConstructor();
		return con;
	}

	private AST directConstructor() throws QueryException {
		AST con = dirElemConstructor();
		con = (con != null) ? con : dirCommentConstructor();
		con = (con != null) ? con : dirPIConstructor();
		return con;
	}

	private AST dirElemConstructor() throws QueryException {
		if ((laSkipWS("</") != null) || (laSkipWS("<?") != null)
				|| (!attemptSkipWS("<"))) {
			return null;
		}
		// skipS();
		AST stag = qnameLiteral(false, true);
		AST elem = new AST(XQ.CompElementConstructor);
		elem.addChild(stag);
		AST cseq = new AST(XQ.ContentSequence);
		elem.addChild(cseq);
		AST att;
		while ((att = dirAttribute()) != null) {
			cseq.addChild(att);
		}
		if (attemptSkipWS("/>")) {
			return elem;
		}
		consume(">");
		push(stag.getValue());
		AST content;
		while ((content = dirElemContent()) != null) {
			cseq.addChild(content);
		}
		consume("</");
		AST etag = qnameLiteral(false, true);
		pop(etag.getValue());
		// skipS();
		consumeSkipWS(">");
		return elem;
	}

	private AST dirAttribute() throws QueryException {
		skipS();
		AST qname = qnameLiteral(true, false);
		if (qname == null) {
			return null;
		}
		skipS();
		consume("=");
		skipS();
		AST att = new AST(XQ.CompAttributeConstructor);
		att.addChild(qname);
		AST cseq = new AST(XQ.ContentSequence);
		att.addChild(cseq);
		AST val;
		if (attempt("\"")) {
			while ((val = quotAttrValue()) != null) {
				cseq.addChild(val);
			}
			consume("\"");
		} else {
			consume("'");
			while ((val = aposAttrValue()) != null) {
				cseq.addChild(val);
			}
			consume("'");
		}
		return att;
	}

	private AST quotAttrValue() throws QueryException {
		Token la = la("\"");
		if (la != null) {
			if (la(la, "\"") != null) {
				consume("\"\"");
				return new AST(XQ.Str, "\"");
			}
			return null;
		}
		return quotAttrValueContent();
	}

	private AST quotAttrValueContent() throws QueryException {
		Token content = laQuotAttrContentChar();
		if (content != null) {
			consume(content);
			return new AST(XQ.Str, content.string());
		}
		return commonContent();
	}

	private AST aposAttrValue() throws QueryException {
		Token la = la("'");
		if (la != null) {
			if (la(la, "'") != null) {
				consume("''");
				return new AST(XQ.Str, "'");
			}
			return null;
		}
		return aposAttrValueContent();
	}

	private AST aposAttrValueContent() throws QueryException {
		Token content = laAposAttrContentChar();
		if (content != null) {
			consume(content);
			return new AST(XQ.Str, content.string());
		}
		return commonContent();
	}

	private AST commonContent() throws QueryException {
		Token c;
		try {
			c = laPredefEntityRef(false);
			c = (c != null) ? c : laCharRef(false);
			c = (c != null) ? c : laEscapeCurly();
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (c != null) {
			consume(c);
			return new AST(XQ.Str, c.string());
		}
		return enclosedExpr();
	}

	private AST dirElemContent() throws QueryException {
		AST c = directConstructor();
		c = (c != null) ? c : cDataSection();
		c = (c != null) ? c : commonContent();
		c = (c != null) ? c : elementContentChar();
		return c;
	}

	private AST cDataSection() throws QueryException {
		if (!attempt("<![CDATA[")) {
			return null;
		}
		Token content = laCDataSectionContents();
		consume(content);
		consume("]]>");
		return new AST(XQ.Str, content.string());
	}

	private AST elementContentChar() {
		Token content = laElemContentChar();
		if (content == null) {
			return null;
		}
		consume(content);
		return new AST(XQ.Str, content.string());
	}

	private AST dirCommentConstructor() throws QueryException {
		if (!attempt("<!--")) {
			return null;
		}
		Token content;
		try {
			content = laCommentContents(false);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		consume(content);
		consume("-->");
		AST comment = new AST(XQ.CompCommentConstructor);
		comment.addChild(new AST(XQ.Str, content.string()));
		return comment;
	}

	private AST dirPIConstructor() throws QueryException {
		// "<?" PITarget (S DirPIContents)? "?>"
		if (!attempt("<?")) {
			return null;
		}
		Token target;
		try {
			target = laPITarget(false);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		consume(target);
		AST piCon = new AST(XQ.DirPIConstructor);
		piCon.addChild(new AST(XQ.PITarget, target.string()));
		if (skipS()) {
			Token content;
			try {
				content = laPIContents();
			} catch (RuntimeException e) {
				throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
						.getMessage());
			}
			consume(content);
			piCon.addChild(new AST(XQ.Str, content.string()));
		}
		consume("?>");
		return piCon;
	}

	private AST computedConstructor() throws QueryException {
		AST c = compDocConstructor();
		c = (c != null) ? c : compElemConstructor();
		c = (c != null) ? c : compAttrConstructor();
		c = (c != null) ? c : compNamespaceConstructor();
		c = (c != null) ? c : compTextConstructor();
		c = (c != null) ? c : compCommentConstructor();
		c = (c != null) ? c : compPIConstructor();
		return c;
	}

	private AST compDocConstructor() throws QueryException {
		Token la = laSkipWS("document");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "{");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST doc = new AST(XQ.CompDocumentConstructor);
		doc.addChild(expr());
		consume("}");
		return doc;
	}

	private AST compElemConstructor() throws QueryException {
		Token la = laSkipWS("element");
		if (la == null) {
			return null;
		}
		Token la2;
		try {
			la2 = laEQNameSkipWS(la, true);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		AST elem;
		if (la2 != null) {
			consume(la);
			consume(la2);
			elem = new AST(XQ.CompElementConstructor);
			elem.addChild(new AST(XQ.QNm, la2.string()));
		} else {
			la2 = laSkipWS(la, "{");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			elem = new AST(XQ.CompElementConstructor);
			elem.addChild(expr());
			consumeSkipWS("}");
		}
		consumeSkipWS("{");
		AST conSeq = new AST(XQ.ContentSequence);
		elem.addChild(conSeq);
		AST expr = expr();
		if (expr != null) {
			conSeq.addChild(expr);
		}
		consumeSkipWS("}");
		return elem;
	}

	private AST compAttrConstructor() throws QueryException {
		Token la = laSkipWS("attribute");
		if (la == null) {
			return null;
		}
		Token la2;
		try {
			la2 = laEQNameSkipWS(la, true);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		AST attr;
		if (la2 != null) {
			consume(la);
			consume(la2);
			attr = new AST(XQ.CompAttributeConstructor);
			attr.addChild(new AST(XQ.QNm, la2.string()));
		} else {
			la2 = laSkipWS(la, "{");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			attr = new AST(XQ.CompAttributeConstructor);
			attr.addChild(expr());
			consumeSkipWS("}");
		}
		consumeSkipWS("{");
		AST conSeq = new AST(XQ.ContentSequence);
		attr.addChild(conSeq);
		AST expr = expr();
		if (expr != null) {
			conSeq.addChild(expr);
		}
		consumeSkipWS("}");
		return attr;
	}

	private AST compNamespaceConstructor() throws QueryException {
		Token la = laSkipWS("namespace");
		if (la == null) {
			return null;
		}
		Token la2;
		try {
			la2 = laNCName(la);
		} catch (RuntimeException e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		AST ns;
		if (la2 != null) {
			consume(la);
			consume(la2);
			ns = new AST(XQ.CompNamespaceConstructor);
			ns.addChild(new AST(XQ.Str, la2.string()));
		} else {
			la2 = laSkipWS(la, "{");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			ns = new AST(XQ.CompNamespaceConstructor);
			ns.addChild(expr());
			consumeSkipWS("}");
		}
		consumeSkipWS("{");
		AST conSeq = new AST(XQ.ContentSequence);
		ns.addChild(conSeq);
		AST expr = expr();
		if (expr != null) {
			conSeq.addChild(expr);
		}
		consumeSkipWS("}");
		return ns;
	}

	private AST compTextConstructor() throws QueryException {
		Token la = laSkipWS("text");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "{");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST doc = new AST(XQ.CompTextConstructor);
		doc.addChild(expr());
		consume("}");
		return doc;
	}

	private AST compCommentConstructor() throws QueryException {
		Token la = laSkipWS("comment");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "{");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST doc = new AST(XQ.CompCommentConstructor);
		doc.addChild(expr());
		consume("}");
		return doc;
	}

	private AST compPIConstructor() throws QueryException {
		Token la = laSkipWS("processing-instruction");
		if (la == null) {
			return null;
		}
		Token la2;
		try {
			la2 = laNCName(la);
		} catch (RuntimeException e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		AST pi;
		if (la2 != null) {
			consume(la);
			consume(la2);
			pi = new AST(XQ.CompProcessingInstructionConstructor);
			pi.addChild(new AST(XQ.Str, la2.string()));
		} else {
			la2 = laSkipWS(la, "{");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			pi = new AST(XQ.CompProcessingInstructionConstructor);
			pi.addChild(expr());
			consumeSkipWS("}");
		}
		consumeSkipWS("{");
		AST conSeq = new AST(XQ.ContentSequence);
		pi.addChild(conSeq);
		AST expr = expr();
		if (expr != null) {
			conSeq.addChild(expr);
		}
		consumeSkipWS("}");
		return pi;
	}

	private AST functionItemExpr() throws QueryException {
		AST funcItem = literalFunctionItem();
		funcItem = (funcItem != null) ? funcItem : inlineFunction();
		return funcItem;
	}

	private AST literalFunctionItem() throws QueryException {
		Token la;
		try {
			la = laEQNameSkipWS(true);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS("#");
		if (la2 == null) {
			return null;
		}
		AST eqname = new AST(XQ.QNm, la.string());
		consume(la);
		consume(la2);
		AST no = integerLiteral(false, true);
		AST litFunc = new AST(XQ.LiteralFuncItem);
		litFunc.addChild(eqname);
		litFunc.addChild(no);
		return litFunc;
	}

	private AST inlineFunction() throws QueryException {
		Token la = laSkipWS("function");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST inlineFunc = new AST(XQ.InlineFuncItem);
		do {
			AST param = param();
			if (param == null) {
				break;
			}
			inlineFunc.addChild(param);
		} while (attemptSkipWS(","));
		consumeSkipWS(")");
		if (attemptSkipWS("as")) {
			inlineFunc.addChild(sequenceType());
		}
		inlineFunc.addChild(enclosedExpr());
		return inlineFunc;
	}

	private AST enclosedExpr() throws QueryException {
		if (!attemptSkipWS("{")) {
			return null;
		}
		AST expr = expr();
		consumeSkipWS("}");
		return expr;
	}

	private AST param() throws QueryException {
		if (!attemptSkipWS("$")) {
			return null;
		}
		AST eqname = eqnameLiteral(false, false);
		String varName = declare(eqname.getValue());
		AST decl = new AST(XQ.TypedVariableDeclaration);
		decl.addChild(new AST(XQ.Variable, varName));
		AST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			decl.addChild(typeDecl);
		}
		return decl;
	}

	private AST stringLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laStringSkipWS(cond) : laString(cond);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected string literal: '%s'", paraphrase());
		}
		consume(la);
		return new AST(XQ.Str, la.string());
	}

	private AST uriLiteral(boolean cond, boolean skipWS) throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laStringSkipWS(cond) : laString(cond);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected URI literal: '%s'", paraphrase());
		}
		consume(la);
		return new AST(XQ.AnyURI, la.string());
	}

	private AST numericLiteral() throws QueryException {
		AST lit = integerLiteral(true, true);
		lit = (lit != null) ? lit : decimalLiteral(true, true);
		lit = (lit != null) ? lit : doubleLiteral(true, true);
		return lit;
	}

	private AST ncnameLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laNCNameSkipWS() : laNCName();
		} catch (RuntimeException e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected NCName: '%s'", paraphrase());
		}
		consume(la);
		return new AST(XQ.Str, la.string());
	}

	private AST eqnameLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laEQNameSkipWS(cond) : laEQName(cond);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected QName: '%s'", paraphrase());
		}
		consume(la);
		return new AST(XQ.QNm, la.string());
	}

	private AST qnameLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laQNameSkipWS() : laQName();
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected QName: '%s'", paraphrase());
		}
		consume(la);
		return new AST(XQ.QNm, la.string());
	}

	private AST doubleLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laDoubleSkipWS(cond) : laDouble(cond);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected double value: '%s'", paraphrase());
		}
		consume(la);
		return new AST(XQ.Dbl, la.string());
	}

	private AST decimalLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laDecimalSkipWS(cond) : laDecimal(cond);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected decimal value: '%s'", paraphrase());
		}
		consume(la);
		return new AST(XQ.Dec, la.string());
	}

	private AST integerLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laIntegerSkipWS(cond) : laInteger(cond);
		} catch (RuntimeException e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected integer value: '%s'", paraphrase());
		}
		consume(la);
		return new AST(XQ.Int, la.string());
	}

	private AST pragmaContent() throws QueryException {
		Token la;
		try {
			la = laPragma(false);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		consume(la);
		return (la == null) ? null : new AST(XQ.PragmaContent, la.string());
	}

	private AST[] add(AST[] asts, AST ast) {
		int len = asts.length;
		asts = Arrays.copyOf(asts, len + 1);
		asts[len] = ast;
		return asts;
	}

	private void push(String name) {

	}

	private void pop(String name) {

	}
}
