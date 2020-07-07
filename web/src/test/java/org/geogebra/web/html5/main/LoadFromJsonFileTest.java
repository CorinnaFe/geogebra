package org.geogebra.web.html5.main;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.geogebra.common.gui.layout.DockManager;
import org.geogebra.common.kernel.geos.GeoBoolean;
import org.geogebra.common.main.App;
import org.geogebra.web.full.gui.layout.panels.ToolbarDockPanelW;
import org.geogebra.web.full.gui.toolbarpanel.ToolbarPanel;
import org.geogebra.web.full.main.AppWFull;
import org.geogebra.web.html5.euclidian.EuclidianSimplePanelW;
import org.geogebra.web.test.AppMocker;
import org.geogebra.web.util.file.FileIO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gwt.dom.client.TextAreaElement;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.gwtmockito.WithClassesToStub;
import com.himamis.retex.renderer.web.graphics.JLMContext2d;

@RunWith(GwtMockitoTestRunner.class)
@WithClassesToStub({TextAreaElement.class, EuclidianSimplePanelW.class,
		JLMContext2d.class, RootPanel.class, ResizeComposite.class})
public class LoadFromJsonFileTest {
	private static final String CLOSED_AV_JSON_PATH =
			"src/test/java/org/geogebra/web/html5/main/closedAV.json";
	private static final String jsonPath =
			"src/test/java/org/geogebra/web/html5/main/inRegion.json";

	private AppWFull app;

	@Test
	public void testLoadApp() {

		TestArticleElement articleElement = new TestArticleElement("prerelease", "simple");
		String json = FileIO.load(jsonPath);
		articleElement.attr("jsonFile", json);
		AppWsimple app = AppMocker.mockAppletSimple(articleElement);
		assertTrue(((GeoBoolean) app.getKernel().lookupLabel("visible")).getBoolean());
	}

	@Test
	public void checkPanelIsClosed() {
		initAppFromFile();
		final ToolbarPanel toolbarPanel = initToolbarFromApp();
		final CountDownLatch latch = new CountDownLatch(1);
		app.invokeLater(new Runnable() {
			@Override
			public void run() {
				Assert.assertTrue(toolbarPanel == null || toolbarPanel.isClosed());
				latch.countDown();
			}
		});
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void initAppFromFile() {
		AppMocker.useProviderForSchedulerImpl();
		TestArticleElement articleElement =
				new TestArticleElement("prerelease", "graphing");
		String json = FileIO.load(CLOSED_AV_JSON_PATH);
		articleElement.attr("jsonFile", json);
		app = AppMocker.mockApplet(articleElement);
		app.setShowToolBar(true);
	}

	private ToolbarPanel initToolbarFromApp() {
		this.getClass().getClassLoader().setDefaultAssertionStatus(false);
		DockManager dockManager = app.getGuiManager().getLayout().getDockManager();
		ToolbarDockPanelW toolbarDockPanel =
				(ToolbarDockPanelW) dockManager.getPanel(App.VIEW_ALGEBRA);
		return toolbarDockPanel.getToolbar();
	}
}
