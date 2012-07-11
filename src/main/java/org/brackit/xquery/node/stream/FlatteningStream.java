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
package org.brackit.xquery.node.stream;

import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Stream;

/**
 * Flattens out a stream of items that provide streams into a single stream.
 * 
 * @author Sebastian Baechle
 * 
 * @param <K>
 * @param <E>
 */
public abstract class FlatteningStream<K, E> implements Stream<E> {
	private Stream<? extends K> in;

	private Stream<? extends E> out;

	private E next;

	public FlatteningStream(Stream<? extends K> in) {
		super();
		this.in = in;
	}

	@Override
	public void close() {
		if (out != null) {
			out.close();
			out = null;
		}
		if (in != null) {
			in.close();
			in = null;
		}
	}

	@Override
	public E next() throws DocumentException {
		try {
			E e;
			while ((out == null) || ((e = out.next()) == null)) {
				if (out != null) {
					out.close();
					out = null;
				}

				if (in == null) {
					return null;
				}

				K k;
				if ((k = in.next()) != null) {
					out = getOutStream(k);
				} else {
					in.close();
					in = null;
					return null;
				}
			}

			return e;
		} catch (DocumentException e) {
			close();
			throw e;
		}
	}

	protected abstract Stream<? extends E> getOutStream(K next)
			throws DocumentException;
}
