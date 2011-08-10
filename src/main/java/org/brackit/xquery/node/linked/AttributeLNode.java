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
package org.brackit.xquery.node.linked;

import org.brackit.xquery.node.parser.SubtreeParser;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.OperationNotSupportedException;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public final class AttributeLNode extends LNode {
	protected String name;

	protected String value;

	AttributeLNode(ParentLNode parent, String name, String value) {
		super(parent);
		this.name = name;
		this.value = value;
	}

	public AttributeLNode(String name, String value) {
		super(null);
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() throws DocumentException {
		return name;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public void setName(String name) throws OperationNotSupportedException,
			DocumentException {
		this.name = name;
	}

	@Override
	public void setValue(String value) throws OperationNotSupportedException,
			DocumentException {
		this.value = value;
	}

	public Kind getKind() {
		return Kind.ATTRIBUTE;
	}

	public boolean isChildOf(Node<?> node) {
		return false;
	}

	@Override
	public boolean isAttributeOf(Node<?> node) {
		return ((parent != null) && (parent == (Object) node));
	}

	@Override
	public boolean isDescendantOf(Node<?> node) {
		return false;
	}

	@Override
	public boolean isDescendantOrSelfOf(Node<?> node) {
		// check only for self
		// TODO: fix for sun's compiler bug using generics parent == node
		return ((node != null) && ((this == (Object) node)));
	}

	@Override
	public boolean isFollowingOf(Node<?> node) {
		return false;
	}

	@Override
	public boolean isFollowingSiblingOf(Node<?> node) {
		return false;
	}

	@Override
	public boolean isPrecedingOf(Node<?> node) {
		return false;
	}

	@Override
	public boolean isPrecedingSiblingOf(Node<?> node) {
		return false;
	}

	@Override
	public boolean isSiblingOf(Node<?> node) {
		return false;
	}

	@Override
	public LNode replaceWith(Kind kind, String value)
			throws OperationNotSupportedException, DocumentException {
		if (kind != Kind.ATTRIBUTE) {
			throw new DocumentException(
					"Cannot replace attribute with node of type: %s.", kind);
		}

		if (parent == null) {
			throw new DocumentException("Cannot replace node without parent");
		}

		return parent.setAttribute(name, value);
	}

	@Override
	public LNode replaceWith(Node<?> node)
			throws OperationNotSupportedException, DocumentException {
		Kind kind = node.getKind();
		if (kind != Kind.ATTRIBUTE) {
			throw new DocumentException(
					"Cannot replace attribute with node of type: %s.", kind);
		}

		if (parent == null) {
			throw new DocumentException("Cannot replace node without parent");
		}

		return parent.setAttribute(name, value);
	}

	@Override
	public LNode replaceWith(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		LNode node = builder.build(parser);
		Kind kind = node.getKind();
		if (kind != Kind.ATTRIBUTE) {
			throw new DocumentException(
					"Cannot replace attribute with node of type: %s.", kind);
		}

		if (parent == null) {
			throw new DocumentException("Cannot replace node without parent");
		}

		return parent.setAttribute(name, value);
	}

	@Override
	public String toString() {
		return String.format("(type='%s', name='%s', value='%s')",
				Kind.ATTRIBUTE, name, value);
	}
}
