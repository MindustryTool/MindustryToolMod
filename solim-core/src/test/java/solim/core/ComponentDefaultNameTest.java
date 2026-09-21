package solim.core;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import solim.display.SolimImage;
import solim.display.Text;
import solim.input.Button;
import solim.input.Checkbox;
import solim.input.SolimSelect;
import solim.input.SolimSlider;
import solim.input.SolimTextField;
import solim.input.Switch;
import solim.layout.Card;
import solim.layout.Column;
import solim.layout.Divider;
import solim.layout.Grid;
import solim.layout.Row;
import solim.layout.Scroll;
import solim.layout.SolimStack;
import solim.layout.Spacer;
import solim.layout.Wrap;
import solim.overlay.SolimDialog;
import solim.reactive.Signal;
import solim.test.SolimEnv;

class ComponentDefaultNameTest extends SolimEnv {


	static class CustomTestCard extends BaseComponent {
		@Override
		protected Element build() {
			return new Table();
		}
	}

	@Test
	void layoutComponentsDefaultNames() {
		assertEquals("solim-column-table", new Column().element().name);
		assertEquals("solim-row-table", new Row().element().name);

		Card card = new Card();
		assertEquals("solim-card-table", card.element().name);
		assertEquals("solim-card-table", card.container().name);

		assertEquals("solim-grid-table", new Grid().element().name);
		assertEquals("solim-divider", new Divider().element().name);
		assertEquals("solim-spacer-table", new Spacer().element().name);
		assertEquals("solim-scroll-pane-outer", new Scroll().element().name);
		assertEquals("solim-stack-stack", new SolimStack().element().name);
		assertEquals("solim-wrap-table", new Wrap().element().name);
	}

	@Test
	void inputAndDisplayWidgetsDefaultNames() {
		assertEquals("solim-button-sizedButton", new Button().element().name);

		if (Core.scene != null) {
			Checkbox cb = new Checkbox("check", Signal.of(false));
			assertEquals("solim-checkbox-checkBox", cb.element().name);
			cb.dispose();

			SolimSlider sl = new SolimSlider(Signal.of(0f), 0f, 1f, 0.1f);
			assertEquals("solim-slider-slider", sl.element().name);
			sl.dispose();

			SolimTextField tf = new SolimTextField(Signal.of(""));
			assertEquals("solim-textfield-textField", tf.element().name);
			tf.dispose();

			Switch sw = new Switch(Signal.of(false));
			assertEquals("solim-switch-switchBox", sw.element().name);
			sw.dispose();

			SolimSelect<String> sel = new SolimSelect<>(Signal.of("a"), Arrays.asList("a", "b"));
			assertEquals("solim-select-selectBox", sel.element().name);
			sel.dispose();

			assertEquals("solim-text-label", new Text("hello").element().name);
			assertEquals("solim-image-image", new SolimImage().element().name);
			assertEquals("solim-dialog-dialog", new SolimDialog().name);
		}
	}

	@Test
	void baseComponentFallbackDefaultName() {
		CustomTestCard customComp = new CustomTestCard();
		assertEquals("solim-customtestcard-table", customComp.element().name);

		BaseComponent anonComp = new BaseComponent() {
			@Override
			protected Element build() {
				return new Element();
			}
		};
		assertEquals("solim-component-element", anonComp.element().name);
	}

	@Test
	void explicitNameOverwritesDefaultNameAcrossComponents() {
		assertEquals("custom-col", new Column().name("custom-col").element().name);
		assertEquals("custom-row", new Row().name("custom-row").element().name);
		assertEquals("custom-card", new Card().name("custom-card").element().name);
		assertEquals("custom-grid", new Grid().name("custom-grid").element().name);
		assertEquals("custom-divider", new Divider().name("custom-divider").element().name);
		assertEquals("custom-spacer", new Spacer().name("custom-spacer").element().name);
		assertEquals("custom-scroll", new Scroll().name("custom-scroll").element().name);
		assertEquals("custom-stack", new SolimStack().name("custom-stack").element().name);
		assertEquals("custom-wrap", new Wrap().name("custom-wrap").element().name);

		assertEquals("custom-btn", new Button().name("custom-btn").element().name);

		if (Core.scene != null) {
			Checkbox cb = new Checkbox("check", Signal.of(false)).name("custom-cb");
			assertEquals("custom-cb", cb.element().name);
			cb.dispose();

			SolimSlider sl = new SolimSlider(Signal.of(0f), 0f, 1f, 0.1f).name("custom-sl");
			assertEquals("custom-sl", sl.element().name);
			sl.dispose();

			SolimTextField tf = new SolimTextField(Signal.of("")).name("custom-tf");
			assertEquals("custom-tf", tf.element().name);
			tf.dispose();

			Switch sw = new Switch(Signal.of(false)).name("custom-sw");
			assertEquals("custom-sw", sw.element().name);
			sw.dispose();

			SolimSelect<String> sel = new SolimSelect<>(Signal.of("a"), Arrays.asList("a", "b")).name("custom-sel");
			assertEquals("custom-sel", sel.element().name);
			sel.dispose();

			assertEquals("custom-txt", new Text("t").name("custom-txt").element().name);
			assertEquals("custom-img", new SolimImage().name("custom-img").element().name);
			SolimDialog dlg = new SolimDialog();
			dlg.name("custom-dialog");
			assertEquals("custom-dialog", dlg.name);
		}
	}

	@Test
	void baseComponentExplicitNameOverwritesDefaultBeforeAndAfterBuild() {
		CustomTestCard beforeBuild = new CustomTestCard();
		beforeBuild.name("custom-before");
		assertEquals("custom-before", beforeBuild.element().name);

		CustomTestCard afterBuild = new CustomTestCard();
		assertEquals("solim-customtestcard-table", afterBuild.element().name);
		afterBuild.name("custom-after");
		assertEquals("custom-after", afterBuild.element().name);
	}
}
