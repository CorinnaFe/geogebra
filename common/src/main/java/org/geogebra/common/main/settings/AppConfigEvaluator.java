package org.geogebra.common.main.settings;

import org.geogebra.common.io.layout.Perspective;
import org.geogebra.common.main.AppConfigDefault;

/**
 * Config for Evaluator
 */
public class AppConfigEvaluator extends AppConfigDefault {

    @Override
    public String getForcedPerspective() {
        return Perspective.EVALUATOR + "";
    }

    @Override
    public String getAppTitle() {
        return "Evaluator";
    }

    @Override
    public String getAppCode() {
        return "evaluator";
    }

    @Override
    public String getTutorialKey() {
        return "evaluator_tutorials";
    }
}
