package org.geogebra.web.full.evaluator;

import org.geogebra.common.plugin.Event;
import org.geogebra.common.plugin.EventType;
import org.geogebra.common.plugin.evaluator.EvaluatorAPI;
import org.geogebra.web.full.gui.components.MathFieldEditor;
import org.geogebra.web.html5.main.AppW;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.himamis.retex.editor.share.editor.MathFieldInternal;
import com.himamis.retex.editor.share.event.MathFieldListener;
import com.himamis.retex.editor.share.model.MathSequence;

/**
 * Evaluator Web implementation.
 *
 * @author Laszlo
 */
public class EvaluatorEditor implements IsWidget, MathFieldListener, BlurHandler {

	private AppW app;
	private MathFieldEditor mathFieldEditor;
	private EvaluatorAPI evaluatorAPI;

	/**
	 * Constructor
	 *
	 * @param app
	 *            The application.
	 */
	public EvaluatorEditor(AppW app) {
		this.app = app;
		mathFieldEditor = new MathFieldEditor(app, this);
		mathFieldEditor.addStyleName("evaluatorEditor");
		mathFieldEditor.addBlurHandler(this);
		mathFieldEditor.setFontSize(app.getArticleElement().getParamFontSize(18));
		mathFieldEditor.setUseKeyboardButton(false);

		MathFieldInternal mathFieldInternal = mathFieldEditor.getMathField().getInternal();
		evaluatorAPI = new EvaluatorAPI(app.getKernel(), mathFieldInternal);
	}

	@Override
	public void onEnter() {
		mathFieldEditor.reset();
	}

	@Override
	public void onKeyTyped() {
		scrollContentIfNeeded();
		Event event = new Event(EventType.EDITOR_KEY_TYPED)
				.setJsonArgument(evaluatorAPI.getEvaluatorValue());
		app.dispatchEvent(event);
	}

	@Override
	public void onCursorMove() {
		scrollContentIfNeeded();
	}

	private void scrollContentIfNeeded() {
		mathFieldEditor.scrollHorizontally();
		mathFieldEditor.scrollVertically();
	}

	@Override
	public void onUpKeyPressed() {
	 	// nothing to do.
	}

	@Override
	public void onDownKeyPressed() {
		// nothing to do.
	}

	@Override
	public String serialize(MathSequence selectionText) {
		return null;
	}

	@Override
	public void onInsertString() {
		scrollContentIfNeeded();
	}

	@Override
	public boolean onEscape() {
		return true;
	}

	@Override
	public void onTab(boolean shiftDown) {
		// TODO: implement this.
	}

	@Override
	public Widget asWidget() {
		return mathFieldEditor.asWidget();
	}

	public void requestFocus() {
		mathFieldEditor.requestFocus();
	}

	/**
	 * @return evaluator API
	 */
	public EvaluatorAPI getAPI() {
		return evaluatorAPI;
	}

	@Override
	public void onBlur(BlurEvent event) {
		mathFieldEditor.setKeyboardVisibility(false);
	}
}
