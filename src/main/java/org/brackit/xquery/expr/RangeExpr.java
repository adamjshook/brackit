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
package org.brackit.xquery.expr;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.sequence.BaseIter;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class RangeExpr implements Expr {
	protected final Expr leftExpr;
	protected final Expr rightExpr;

	public RangeExpr(Expr leftExpr, Expr rightExpr) {
		this.leftExpr = leftExpr;
		this.rightExpr = rightExpr;
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		Item lItem = leftExpr.evaluateToItem(ctx, tuple);
		Item rItem = rightExpr.evaluateToItem(ctx, tuple);

		if ((lItem == null) || (rItem == null)) {
			return null;
		}

		Atomic left = lItem.atomize();
		Atomic right = rItem.atomize();

		if (!(left instanceof IntNumeric)) {
			left = Cast.cast(left, Type.INT, false);
		}

		if (!(right instanceof IntNumeric)) {
			right = Cast.cast(right, Type.INT, false);
		}

		int comparison = left.cmp(right);
		if (comparison > 0) {
			return null;
		} else if (comparison == 0) {
			return left;
		} else {
			throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
		}
	}

	@Override
	public Sequence evaluate(QueryContext ctx, Tuple tuple)
			throws QueryException {
		Item lItem = leftExpr.evaluateToItem(ctx, tuple);
		Item rItem = rightExpr.evaluateToItem(ctx, tuple);

		if ((lItem == null) || (rItem == null)) {
			return null;
		}

		Atomic left = lItem.atomize();
		Atomic right = rItem.atomize();

		if (!(left instanceof IntNumeric)) {
			left = Cast.cast(left, Type.INT, false);
		}

		if (!(right instanceof IntNumeric)) {
			right = Cast.cast(right, Type.INT, false);
		}

		int comparison = left.cmp(right);
		if (comparison > 0) {
			return null;
		} else if (comparison == 0) {
			return left;
		} else {
			final IntNumeric s = (IntNumeric) left;
			final IntNumeric e = (IntNumeric) right;

			return new Sequence() {
				private final IntNumeric start = s;
				private final IntNumeric end = e;

				@Override
				public boolean booleanValue()
						throws QueryException {
					if (!size().eq(Int32.ONE)) {
						throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
								"Effective boolean value is undefined "
								+ "for sequences with two or more items "
								+ "not starting with a node");
					}
					return start.booleanValue();
				}

				@Override
				public IntNumeric size()
						throws QueryException {
					return (IntNumeric) end.subtract(start).add(Int32.ONE);
				}

				@Override
				public Iter iterate() {
					return new BaseIter() {
						IntNumeric current = start;

						@Override
						public void close() {
						}

						@Override
						public Item next() throws QueryException {
							if (current.cmp(e) > 0)
								return null;

							IntNumeric res = current;
							current = current.inc();
							return res;
						}

						@Override
						public void skip(IntNumeric i) throws QueryException {
							if (i.cmp(Int32.ZERO) <= 0) {
								return;
							}
							current.add(i);
						}
					};
				}

				@Override
				public Item get(IntNumeric pos) throws QueryException {
					if (Int32.ZERO.cmp(pos) >= 0) {
						return null;
					}					
					if (size().cmp(pos) < 0) {
						return null;
					}
					return (IntNumeric) start.add(pos).subtract(Int32.ONE);
				}
			};
		}
	}

	@Override
	public boolean isUpdating() {
		return ((leftExpr.isUpdating()) || (rightExpr.isUpdating()));
	}

	@Override
	public boolean isVacuous() {
		return false;
	}

	public String toString() {
		return "(" + leftExpr + " to " + rightExpr + ")";
	}
}
