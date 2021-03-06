// Metawidget
//
// This file is dual licensed under both the LGPL
// (http://www.gnu.org/licenses/lgpl-2.1.html) and the EPL
// (http://www.eclipse.org/org/documents/epl-v10.php). As a
// recipient of Metawidget, you may choose to receive it under either
// the LGPL or the EPL.
//
// Commercial licenses are also available. See http://metawidget.org
// for details.

package org.metawidget.layout.decorator;

import java.util.Map;

import org.metawidget.layout.iface.AdvancedLayout;
import org.metawidget.layout.iface.Layout;
import org.metawidget.layout.iface.LayoutException;

/**
 * Decorates a Layout with additional functionality.
 * <p>
 * LayoutDecorator is abstract. Unlike CompositeInspector and CompositeWidgetBuilder, there is no
 * equivalent CompositeLayout for Layouts. This is because most Layouts are 'end points' and cannot
 * sensibly be composed into Lists. It is unclear what should happen if, say, a CompositeLayout
 * combined a GridBagLayout with a FlowLayout. Rather, Layouts must be combined in a
 * <em>heirarchical</em> fashion, with an 'outer' Layout delegating to a single 'inner' Layout.
 * <p>
 * This delegation approach allows us to extract, say, TabbedPaneLayoutDecorator such that you can
 * decorate other layouts (eg. GridBagLayout, GroupLayout etc) with tabbed section functionality. It
 * also makes it possible to configure whether 'sections within sections' should be rendered as
 * 'tabs within tabs' or 'headings within tabs'.
 * <p>
 * Note: the name Layout<em>Decorator</em> refers to the Decorator design pattern.
 *
 * @author <a href="http://kennardconsulting.com">Richard Kennard</a>
 */

public abstract class LayoutDecorator<W, C extends W, M extends C>
	implements AdvancedLayout<W, C, M> {

	//
	// Private members
	//

	private final Layout<W, C, M>	mDelegate;

	//
	// Constructor
	//

	/**
	 * Constructor.
	 * <p>
	 * <code>LayoutDecoratorConfig</code> is mandatory because all <code>LayoutDecorator</code>s
	 * need something to decorate.
	 */

	protected LayoutDecorator( LayoutDecoratorConfig<W, C, M> config ) {

		mDelegate = config.getLayout();

		if ( mDelegate == null ) {
			throw LayoutException.newException( getClass().getName() + " needs a Layout to decorate (use " + config.getClass().getName() + ".setLayout)" );
		}
	}

	//
	// Public methods
	//

	public void onStartBuild( M metawidget ) {

		if ( getDelegate() instanceof AdvancedLayout<?, ?, ?> ) {
			( (AdvancedLayout<W, C, M>) getDelegate() ).onStartBuild( metawidget );
		}
	}

	public void startContainerLayout( C container, M metawidget ) {

		if ( getDelegate() instanceof AdvancedLayout<?, ?, ?> ) {
			( (AdvancedLayout<W, C, M>) getDelegate() ).startContainerLayout( container, metawidget );
		}
	}

	public void layoutWidget( W component, String elementName, Map<String, String> attributes, C container, M metawidget ) {

		getDelegate().layoutWidget( component, elementName, attributes, container, metawidget );
	}

	public void endContainerLayout( C container, M metawidget ) {

		if ( getDelegate() instanceof AdvancedLayout<?, ?, ?> ) {
			( (AdvancedLayout<W, C, M>) getDelegate() ).endContainerLayout( container, metawidget );
		}
	}

	public void onEndBuild( M metawidget ) {

		if ( getDelegate() instanceof AdvancedLayout<?, ?, ?> ) {
			( (AdvancedLayout<W, C, M>) getDelegate() ).onEndBuild( metawidget );
		}
	}

	//
	// Protected methods
	//

	protected Layout<W, C, M> getDelegate() {

		return mDelegate;
	}
}
