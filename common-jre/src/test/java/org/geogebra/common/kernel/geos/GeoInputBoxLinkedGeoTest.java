package org.geogebra.common.kernel.geos;

import static org.junit.Assert.assertEquals;

import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.factories.AwtFactoryCommon;
import org.geogebra.common.jre.headless.AppCommon;
import org.geogebra.common.jre.headless.LocalizationCommon;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.main.AppCommon3D;
import org.geogebra.common.plugin.GeoClass;
import org.geogebra.test.commands.AlgebraTestHelper;
import org.junit.Assert;
import org.junit.Test;

import com.himamis.retex.editor.share.util.Unicode;

public class GeoInputBoxLinkedGeoTest extends BaseUnitTest {

	private GeoInputBox inputBox;

	@Override
	public AppCommon createAppCommon() {
		return new AppCommon3D(new LocalizationCommon(3),
				new AwtFactoryCommon());
	}

	@Test
	public void shouldNotShowQuotesForText() {
		setupInput("txt", "\"GeoGebra Rocks\"");
		t("ib", "GeoGebra Rocks");
		updateInput("GeoGebra Really Rocks");
		t("txt", "GeoGebra Really Rocks");
		hasType("txt", GeoClass.TEXT);
	}

	@Test
	public void shouldShowNewlineQuotesForText() {
		setupInput("txt", "\"GeoGebra\\\\nRocks\"");
		assertEquals("GeoGebra\\\\nRocks", inputBox.getText());
		updateInput("GeoGebra\\\\nReally\\\\nRocks");
		t("txt", "GeoGebra\nReally\nRocks");
	}

	@Test
	public void enteringNewValueShouldKeepVectorType() {
		setupAndCheckInput("v", "(1, 3)");
		t("Rename(v,\"V\")", new String[0]);
		updateInput("(1, 5)");
		t("V", "(1, 5)");
		hasType("V", GeoClass.VECTOR);
	}

	@Test
	public void enteringNewValueShouldKeepVectorType3D() {
		setupAndCheckInput("v3", "(1, 3, 6)");
		updateInput("(1, 5)");
		t("v3", "(1, 5, 0)");
		hasType("v3", GeoClass.VECTOR3D);
	}

	@Test
	public void enteringNewValueShouldKeepPlaneType() {
		setupAndCheckInput("p", "x + y - z = 0");
		updateInput("x = y");
		t("p", "x - y = 0");
		hasType("p", GeoClass.PLANE3D);
	}

	@Test
	public void enteringNewValueShouldKeepComplexNumber() {
		setupAndCheckInput("P", "1 + i");
		updateInput("7");
		t("P", "7 + 0" + Unicode.IMAGINARY);
		assertEquals("7",
				lookup("P").getDefinition(StringTemplate.defaultTemplate));
		hasType("P", GeoClass.POINT);
	}

	@Test
	public void enteringShortLinearExprShouldKeepLineType() {
		setupAndCheckInput("l", "y = 2x + 3");
		updateInput("3x + 5");
		t("l", "y = 3x + 5");
		hasType("l", GeoClass.LINE);
	}

	@Test
	public void symbolicShouldShowDefinition() {
		setupInput("l", "1 + 1 / 5");
		((GeoNumeric) lookup("l")).setSymbolicMode(true, false);
		inputBox.setSymbolicMode(true, false);
		assertEquals("1+1/5", inputBox.getTextForEditor());
		((GeoNumeric) lookup("l")).setSymbolicMode(false, false);
		assertEquals("1+1/5", inputBox.getTextForEditor());
	}

	@Test
	public void nonsymbolicShouldShowDefinitionForFraction() {
		setupInput("l", "1 + 1 / 5");
		((GeoNumeric) lookup("l")).setSymbolicMode(true, true);
		inputBox.setSymbolicMode(false, false);
		assertEquals("6 / 5", inputBox.getText());
		((GeoNumeric) lookup("l")).setSymbolicMode(false, true);
		assertEquals("1.2", inputBox.getText());
	}

	@Test
	public void shouldShowValueForSimpleNumeric() {
		setupInput("l", "5");
		inputBox.setSymbolicMode(true, false);
		assertEquals("5", inputBox.getText());
		assertEquals("5", inputBox.getTextForEditor());
	}

	@Test
	public void shouldBeEmptyAfterSettingLineUndefined() {
		setupInput("f", "y = 5");
		t("SetValue(f, ?)");
		assertEquals("", inputBox.getText());
	}

	@Test
	public void symbolicShouldBeEmptyAfterSettingLineUndefined() {
		setupInput("f", "y = 5");
		t("SetValue(f, ?)");
		inputBox.setSymbolicMode(true, false);
		assertEquals("", inputBox.getText());
		assertEquals("", inputBox.getTextForEditor());
	}

	@Test
	public void shouldBeEmptyAfterSettingPlaneUndefined() {
		setupInput("eq1", "4x + 3y + 2z = 1");
		t("SetValue(eq1, ?)");
		assertEquals("", inputBox.getText());
	}

	@Test
	public void symbolicShouldBeEmptyAfterSettingPlaneUndefined() {
		setupInput("eq1", "4x + 3y + 2z = 1");
		t("SetValue(eq1, ?)");
		inputBox.setSymbolicMode(true, false);
		assertEquals("", inputBox.getText());
		assertEquals("", inputBox.getTextForEditor());
	}

	@Test
	public void shouldBeEmptyAfterPlaneInputUndefined() {
		setupInput("eq1", "4x + 3y + 2z = 1");
		GeoElement ib2 = add("in2=InputBox(eq1)");
		updateInput("?");
		// both input boxes undefined, but first one remembers user input ...
		assertEquals("?", inputBox.getText());
		// ... and second one stays empty (APPS-1246)
		assertEquals("", ((GeoInputBox) ib2).getText());
	}

	@Test
	public void shouldBeEmptyAfterSettingComplexUndefined() {
		setupInput("z1", "3 + i");
		t("SetValue(z1, ?)");
		assertEquals("", inputBox.getText());
	}

	@Test
	public void symbolicShouldBeEmptyAfterSettingComplexUndefined() {
		setupInput("z1", "3 + i");
		t("SetValue(z1, ?)");
		inputBox.setSymbolicMode(true, false);
		assertEquals("", inputBox.getText());
		assertEquals("", inputBox.getTextForEditor());
	}

	@Test
	public void functionParameterShouldNotChangeToX() {
		add("f(c) = c / ?");
		inputBox = add("ib=InputBox(f)");
		inputBox.setSymbolicMode(false, false);
		assertEquals("c / ?", inputBox.getText());
		updateInput("?");
		assertEquals("", inputBox.getText());
		updateInput("c / 3");
		assertEquals("c / 3", inputBox.getText());
	}

	@Test
	public void independentVectorsMustBeColumnEditable() {
		setupInput("l", "(1, 2, 3)");
		assertEquals("{{1}, {2}, {3}}", inputBox.getTextForEditor());
	}

	@Test
	public void symbolicShouldSupportVectorsWithVariables() {
		add("a: 1");
		setupInput("l", "(1, 2, a)");
		assertEquals("(1, 2, a)", inputBox.getText());
		assertEquals("{{1}, {2}, {a}}", inputBox.getTextForEditor());
	}

	@Test
	public void compound2DVectorsMustBeFlatEditable() {
		add("u: (1, 2)");
		add("v: (3, 4)");
		setupInput("l", "u + v");
		assertEquals("u+v", inputBox.getTextForEditor());
	}

	@Test
	public void compound3DVectorsMustBeFlatEditable() {
		add("u: (1, 2, 3)");
		add("v: (3, 4, 5)");
		setupInput("l", "u + v");
		assertEquals("u+v", inputBox.getTextForEditor());
	}

	@Test
	public void twoVariableFunctionParameterShouldNotChangeToX() {
		add("g(p, q) = p / ?");
		inputBox = add("ib=InputBox(g)");
		inputBox.setSymbolicMode(false, false);
		assertEquals("p / ?", inputBox.getText());
		updateInput("?");
		assertEquals("", inputBox.getText());
		updateInput("p / q");
		assertEquals("p / q", inputBox.getText());
	}

	@Test
	public void testGeoNumericExtendsMinMax() {
		GeoNumeric numeric = add("a = 5");
		numeric.setShowExtendedAV(true);
		numeric.initAlgebraSlider();
		Assert.assertFalse(numeric.getIntervalMax() >= 20);
		Assert.assertFalse(numeric.getIntervalMin() <= -20);

		GeoInputBox inputBox = add("ib = InputBox(a)");
		inputBox.updateLinkedGeo("20");
		inputBox.updateLinkedGeo("-20");
		Assert.assertTrue(numeric.getIntervalMax() >= 20);
		Assert.assertTrue(numeric.getIntervalMin() <= -20);
	}

	private void t(String input, String... expected) {
		AlgebraTestHelper.testSyntaxSingle(input, expected,
				getApp().getKernel().getAlgebraProcessor(),
				StringTemplate.xmlTemplate);
	}

	private void hasType(String label, GeoClass geoClass) {
		assertEquals(lookup(label).getGeoClassType(), geoClass);
	}

	private void updateInput(String string) {
		inputBox.textObjectUpdated(new ConstantTextObject(string));
	}

	private void setupAndCheckInput(String label, String value) {
		setupInput(label, value);
		assertEquals(value,
				inputBox.toValueString(StringTemplate.testTemplate));
	}

	private void setupInput(String label, String value) {
		add(label + ":" + value);
		inputBox = add("ib=InputBox(" + label + ")");
	}

	@Test
	public void testCanBeSymbolicForPlane() {
		add("A = (0,0)");
		add("B = (2,0)");
		add("C = (2,2)");
		add("p:Plane(A,B,C)");
		GeoInputBox inputBox = add("InputBox(p)");
		Assert.assertTrue(inputBox.canBeSymbolic());
	}

	@Test
	public void testCanBeSymbolicForEquation() {
		add("eq1:x^3+y^3=1");
		GeoInputBox inputBox1 = add("InputBox(eq1)");
		add("eq2:x^2+y^2+z^2=1");
		GeoInputBox inputBox2 = add("InputBox(eq2)");
		Assert.assertTrue(inputBox1.canBeSymbolic());
		Assert.assertTrue(inputBox2.canBeSymbolic());
	}
}
