package org.geogebra.common.euclidian.draw;

import java.util.ArrayList;
import java.util.List;

import org.geogebra.common.awt.GAffineTransform;
import org.geogebra.common.awt.GPoint2D;
import org.geogebra.common.awt.GRectangle;
import org.geogebra.common.awt.GShape;
import org.geogebra.common.euclidian.BoundingBox;
import org.geogebra.common.euclidian.Drawable;
import org.geogebra.common.euclidian.EuclidianBoundingBoxHandler;
import org.geogebra.common.euclidian.EuclidianView;
import org.geogebra.common.euclidian.RotatableBoundingBox;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoWidget;
import org.geogebra.common.kernel.geos.RectangleTransformable;

public abstract class DrawWidget extends Drawable {

	private final TransformableRectangle rectangle;

	/**
	 * @param view view
	 * @param geo construction element
	 */
	public DrawWidget(EuclidianView view, GeoElement geo) {
		super(view, geo);
		this.rectangle = new TransformableRectangle(view, (RectangleTransformable) geo);
	}

	protected void updateBounds() {
		rectangle.updateSelfAndBoundingBox();
	}

	@Override
	public boolean hit(int x, int y, int hitThreshold) {
		return rectangle.hit(x, y);
	}

	@Override
	public boolean isInside(GRectangle rect) {
		return rect.contains(getBounds());
	}

	@Override
	public GRectangle getBounds() {
		return rectangle.getBounds();
	}

	@Override
	public RotatableBoundingBox getBoundingBox() {
		return rectangle.getBoundingBox();
	}

	@Override
	public BoundingBox<? extends GShape> getSelectionBoundingBox() {
		return getBoundingBox();
	}

	/**
	 * @param newWidth
	 *            pixel width at current zoom
	 */
	public void setWidth(int newWidth) {
		getGeoElement().setWidth(newWidth);
	}

	/**
	 * @param newHeight
	 *            pixel height at current zoom
	 */
	public void setHeight(int newHeight) {
		getGeoElement().setHeight(newHeight);
	}

	/**
	 * @return width on screen at current zoom
	 */
	public final double getWidth() {
		return getGeoElement().getWidth();
	}

	/**
	 * @return height on screen at current zoom
	 */
	public final double getHeight() {
		return getGeoElement().getHeight();
	}

	/**
	 * @return left corner x-coord in EV
	 */
	public final double getLeft() {
		return view.toScreenCoordX(getGeoElement().getLocation().getX());
	}

	/**
	 * @return top corner y-coord in EV
	 */
	public final double getTop() {
		return view.toScreenCoordY(getGeoElement().getLocation().getY());
	}

	@Override
	public List<GPoint2D> toPoints() {
		return rectangle.toPoints();
	}

	@Override
	public void fromPoints(ArrayList<GPoint2D> pts) {
		rectangle.fromPoints(pts);
	}

	@Override
	public void updateByBoundingBoxResize(GPoint2D point,
			EuclidianBoundingBoxHandler handler) {
		rectangle.updateByBoundingBoxResize(point, handler);
	}

	@Override
	public abstract GeoWidget getGeoElement();

	/**
	 * @return whether aspect ratio is fixed for this widget
	 */
	public abstract boolean isFixedRatio();

	/**
	 * @return embed ID
	 */
	public abstract int getEmbedID();

	public abstract boolean isBackground();

	public abstract void setBackground(boolean b);

	public GAffineTransform getTransform() {
		return rectangle.getDirectTransform();
	}

	public GPoint2D getInversePoint(int x, int y) {
		return rectangle.getInversePoint(x, y);
	}
}
