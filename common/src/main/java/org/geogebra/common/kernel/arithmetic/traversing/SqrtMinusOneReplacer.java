package org.geogebra.common.kernel.arithmetic.traversing;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.arithmetic.ExpressionNode;
import org.geogebra.common.kernel.arithmetic.ExpressionValue;
import org.geogebra.common.kernel.arithmetic.Traversing;
import org.geogebra.common.plugin.Operation;

public 	class SqrtMinusOneReplacer implements Traversing {
	private final Kernel kernel;

	public SqrtMinusOneReplacer(Kernel kernel) {
		this.kernel = kernel;
	}

	@Override
	public ExpressionValue process(ExpressionValue ev) {
		ExpressionNode node = ev.wrap();
		ExpressionValue left = node.getLeft();
		if (node.getOperation() == Operation.SQRT
				&& left.isNumberValue() && left.isConstant()) {
			if (left.evaluateDouble() == -1) {
				return kernel.getImaginaryUnit();
			}
		}
		return ev;
	}
}
