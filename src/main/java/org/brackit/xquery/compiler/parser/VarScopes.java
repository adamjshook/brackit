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

import java.util.HashMap;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class VarScopes {
	private int idSequence;

	private Scope root = new Scope(null);

	private Scope current = root;
	
	private Scope resolveIn = root;

	private int level;

	private class Scope {
		Scope parent;

		HashMap<String, Variable> mapping = new HashMap<String, Variable>();

		Scope(Scope parent) {
			this.parent = parent;
		}

		public String toString() {
			return mapping.toString();
		}
	}

	public class Variable {
		String name;

		public Variable(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}
	}
	
	public int scopeCount() {
		return level;
	}

	public void openScope() {
		level++;
		current = new Scope(current);
	}
	
	public void offerScope() {		
		resolveIn = current;
	}

	public void closeScope() throws QueryException {
		if (level == 0) {
			throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR);
		}
		level--;
		current = current.parent;
	}

	public String declare(String name) throws QueryException {
		if (current.mapping.containsKey(name)) {
			throw new QueryException(ErrorCode.ERR_DUPLICATE_VARIABLE_DECL,
					"Variable $%s has already been declared.", name);
		}

		Variable var = (level > 0) ? new Variable(name + ";" + idSequence++)
				: new Variable(name);
		current.mapping.put(name, var);
		return var.name;
	}

	public String resolve(String name) throws QueryException {
		Scope scope = resolveIn;
		Variable var = null;

		while (((var = scope.mapping.get(name)) == null)
				&& ((scope = scope.parent) != null))
			;

		if (var == null) {
			throw new QueryException(ErrorCode.ERR_UNDEFINED_REFERENCE,
					"Variable $%s has not been declared.", name);
		}

		return var.name;
	}
}
