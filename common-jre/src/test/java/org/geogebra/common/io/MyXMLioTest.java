package org.geogebra.common.io;

import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.main.settings.AppConfigGraphing;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class MyXMLioTest extends BaseUnitTest {

	@Test
	public void testXmlContainsAppCode() {
		MyXMLio myXMLio = Mockito.mock(MyXMLio.class, Mockito
				.withSettings()
				.defaultAnswer(Mockito.CALLS_REAL_METHODS)
				.useConstructor(getKernel(), getConstruction()));
		getApp().setConfig(new AppConfigGraphing());
		String fullXml = myXMLio.getFullXML();
		Assert.assertTrue(fullXml.contains("app=\"graphing\""));
	}
}
