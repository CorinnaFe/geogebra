package org.geogebra.web.full.main.activity;

import org.geogebra.common.main.settings.AppConfigEvaluator;
import org.geogebra.common.plugin.evaluator.EvaluatorAPI;
import org.geogebra.web.full.evaluator.EvaluatorEditor;
import org.geogebra.web.html5.gui.GeoGebraFrameW;
import org.geogebra.web.html5.main.ApiExporter;
import org.geogebra.web.html5.main.AppW;

/**
 * Evaluator Activity.
 */
public class EvaluatorActivity extends BaseActivity {

    private EvaluatorEditor editor;

    /**
     * Activity for evaluator app
     */
    public EvaluatorActivity() {
        super(new AppConfigEvaluator());
    }

	@Override
	public void start(AppW appW) {
		super.start(appW);
		editor = new EvaluatorEditor(appW);
		GeoGebraFrameW frame = appW.getAppletFrame();
		frame.clear();
		frame.add(editor);

		if (!appW.getArticleElement().preventFocus()) {
			editor.requestFocus();
		}
	}

    @Override
    public ApiExporter getApiExporter() {
        // not started yet -> pass the whole activity to geteditor later
        return new EvaluatorApiExporter(this);
    }

    /**
     * @return editor API
     */
    public EvaluatorAPI getEditorAPI() {
        return editor.getAPI();
    }
}
