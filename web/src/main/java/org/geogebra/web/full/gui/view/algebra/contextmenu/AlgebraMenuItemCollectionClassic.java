package org.geogebra.web.full.gui.view.algebra.contextmenu;

import org.geogebra.web.full.gui.view.algebra.AlgebraViewW;
import org.geogebra.web.full.gui.view.algebra.contextmenu.item.SolveItem;

/**
 * AV menu items for Classic
 */
public class AlgebraMenuItemCollectionClassic extends AlgebraMenuItemCollection {

	/**
	 * @param algebraView algebra view
	 */
	public AlgebraMenuItemCollectionClassic(AlgebraViewW algebraView) {
		super(algebraView);
		addAction(0, new SolveItem());
	}
}
