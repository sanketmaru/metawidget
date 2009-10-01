// Metawidget
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package org.metawidget.faces.component;

import static org.metawidget.inspector.InspectionResultConstants.*;
import static org.metawidget.inspector.faces.FacesInspectionResultConstants.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.metawidget.config.ConfigReader;
import org.metawidget.faces.FacesUtils;
import org.metawidget.iface.MetawidgetException;
import org.metawidget.inspector.iface.Inspector;
import org.metawidget.layout.iface.Layout;
import org.metawidget.mixin.w3c.MetawidgetMixin;
import org.metawidget.util.ClassUtils;
import org.metawidget.util.CollectionUtils;
import org.metawidget.util.LogUtils;
import org.metawidget.util.XmlUtils;
import org.metawidget.util.simple.StringUtils;
import org.metawidget.widgetbuilder.iface.WidgetBuilder;
import org.metawidget.widgetprocessor.iface.WidgetProcessor;
import org.w3c.dom.Document;

/**
 * Base Metawidget for Java Server Faces environments.
 * <p>
 * Note: <code>UIMetawidget</code> only extends <code>UIComponentBase</code>. It is not:
 * <p>
 * <ul>
 * <li>a <code>UIInput</code>, though it may contain input widgets
 * <li>a <code>UIOutput</code>, though it may contain output widgets
 * <li>a <code>ValueHolder</code>, as it does not use a <code>Converter</code>
 * </ul>
 * <p>
 * Its default RendererType is <code>table</code>.
 *
 * @author Richard Kennard
 */

@SuppressWarnings( "deprecation" )
public abstract class UIMetawidget
	extends UIComponentBase
{
	//
	// Public statics
	//

	/**
	 * Component-level attribute used to store metadata.
	 */

	public final static String	COMPONENT_ATTRIBUTE_METADATA		= "metawidget-metadata";

	/**
	 * Component-level attribute used to prevent recreation.
	 * <p>
	 * By default, Metawidget destroys and recreates every component after
	 * <code>processUpdates</code> and before <code>encodeBegin</code>. This allows components to
	 * update to reflect changed state in underlying business objects. For example, components may
	 * change from being <code>UIOutput</code> labels to <code>UIInput</code> text boxes after the
	 * user clicks <code>Edit</code>.
	 * <p>
	 * Most components work well with this approach. Some, however, maintain internal state that
	 * would get lost if the component was destroyed and recreated. For example, the ICEfaces
	 * <code>SelectInputDate</code> component keeps its popup state internally. If it is destroyed
	 * and recreated, the popup never appears.
	 * <p>
	 * Such components can be marked with <code>COMPONENT_ATTRIBUTE_NOT_RECREATABLE</code> to
	 * prevent their destruction and recreation. Of course, this somewhat impacts their flexibility.
	 * For example, a <code>SelectInputDate</code> could not change its date format in response to
	 * another component on the form.
	 * <p>
	 * <code>COMPONENT_ATTRIBUTE_NOT_RECREATABLE</code> is also used to mark components that
	 * override default component generation, such as a <code>UIStub</code> used to suppress a
	 * field.
	 */

	public final static String	COMPONENT_ATTRIBUTE_NOT_RECREATABLE	= "metawidget-not-recreatable";

	//
	// Private statics
	//

	/**
	 * Application-level attribute used to cache ConfigReader.
	 */

	private final static String	APPLICATION_ATTRIBUTE_CONFIG_READER	= "metawidget-config-reader";

	private final static String	DEFAULT_USER_CONFIG					= "metawidget.xml";

	//
	// Private members
	//

	private Object				mValue;

	private String				mConfig								= DEFAULT_USER_CONFIG;

	private boolean				mNeedsConfiguring					= true;

	private boolean				mInspectFromParent;

	private Boolean				mReadOnly;

	private Map<Object, Object>	mClientProperties;

	private UIMetawidgetMixin	mMetawidgetMixin;

	//
	// Constructor
	//

	public UIMetawidget()
	{
		mMetawidgetMixin = newMetawidgetMixin();

		// Default renderer

		setRendererType( "table" );
	}

	//
	// Public methods
	//

	@Override
	public String getFamily()
	{
		return "org.metawidget";
	}

	public void setValue( Object value )
	{
		mValue = value;
	}

	public Object getValue()
	{
		if ( mValue != null )
			return mValue;

		ValueBinding valueBinding = getValueBinding( "value" );

		if ( valueBinding == null )
			return null;

		return valueBinding.getValue( getFacesContext() );
	}

	public boolean isReadOnly()
	{
		// Dynamic read-only (takes precedence if set)

		ValueBinding bindingReadOnly = getValueBinding( "readOnly" );

		if ( bindingReadOnly != null )
			return (Boolean) bindingReadOnly.getValue( getFacesContext() );

		// Static read only

		if ( mReadOnly != null )
			return mReadOnly.booleanValue();

		// Default to read-write

		return false;
	}

	public void setReadOnly( Boolean readOnly )
	{
		mReadOnly = readOnly;
	}

	public void setConfig( String config )
	{
		mConfig = config;
		mNeedsConfiguring = true;
	}

	public void setInspector( Inspector inspector )
	{
		mMetawidgetMixin.setInspector( inspector );
	}

	public void setWidgetBuilder( WidgetBuilder<UIComponent, UIMetawidget> widgetBuilder )
	{
		mMetawidgetMixin.setWidgetBuilder( widgetBuilder );
	}

	public void setWidgetProcessors( WidgetProcessor<UIComponent, UIMetawidget>... widgetProcessors )
	{
		mMetawidgetMixin.setWidgetProcessors( CollectionUtils.newArrayList( widgetProcessors ));
	}

	public <T> T getWidgetProcessor( Class<T> widgetProcessorClass )
	{
		return mMetawidgetMixin.getWidgetProcessor( widgetProcessorClass );
	}

	public void setLayout( Layout<UIComponent, UIMetawidget> layout )
	{
		mMetawidgetMixin.setLayout( layout );
	}

	public Layout<UIComponent, UIMetawidget> getLayout()
	{
		return mMetawidgetMixin.getLayout();
	}

	/**
	 * Instructs the Metawidget to inspect the value binding from the parent.
	 * <p>
	 * If the value binding is of the form <code>#{foo.bar}</code>, sometimes
	 * <code>foo.getBar()</code> has useful metadata (such as <code>UiLookup</code>). Metawidget
	 * inspects from parent anyway if <code>#{foo.bar}</code> evaluates to <code>null</code>, but on
	 * occasion it may be necessary to specify parent inspection explicitly.
	 * <p>
	 * The disadvantage of inspecting from parent (and the reason it is not enabled by default) is
	 * that some Inspectors will not know the parent and so not be able to return anything. For
	 * example, HibernateInspector only knows the Hibernate XML mapping files of persistent classes,
	 * not the business class of a UI controller, so asking HibernateInspector to inspect
	 * <code>#{controller.current}</code> from its parent will always return <code>null</code>.
	 */

	public void setInspectFromParent( boolean inspectFromParent )
	{
		mInspectFromParent = inspectFromParent;
	}

	/**
	 * @return the text of the label. This may itself contain a value expression, such as
	 *         <code>UiLabel( "#{foo.name}'s name" )</code>
	 */

	public String getLabelString( Map<String, String> attributes )
	{
		if ( attributes == null )
			return "";

		// Explicit label

		String label = attributes.get( LABEL );

		if ( label != null )
		{
			// (may be forced blank)

			if ( "".equals( label ) )
				return null;

			// (localize if possible)

			String localized = getLocalizedKey( StringUtils.camelCase( label ) );

			if ( localized != null )
				return localized.trim();

			return label.trim();
		}

		// Default name

		String name = attributes.get( NAME );

		if ( name != null )
		{
			// (localize if possible)

			String localized = getLocalizedKey( name );

			if ( localized != null )
				return localized.trim();

			return StringUtils.uncamelCase( name );
		}

		return "";
	}

	/**
	 * @return null if no bundle, ???key??? if bundle is missing a key
	 */

	public String getLocalizedKey( String key )
	{
		String localizedKey = null;
		FacesContext context = FacesContext.getCurrentInstance();
		Application application = context.getApplication();
		String appBundle = application.getMessageBundle();

		// Component-specific bundle

		ValueBinding bindingBundle = getValueBinding( "bundle" );

		if ( bindingBundle != null )
		{
			// (watch out when localizing blank labels)

			if ( key == null || key.trim().length() == 0 )
				return "";

			@SuppressWarnings( "unchecked" )
			Map<String, String> bundleMap = (Map<String, String>) bindingBundle.getValue( context );

			// (check for containsKey first, because BundleMap will return a dummy value otherwise)

			if ( bundleMap.containsKey( key ) )
				localizedKey = bundleMap.get( key );
		}

		// App-specific bundle

		else if ( appBundle != null )
		{
			try
			{
				localizedKey = ResourceBundle.getBundle( appBundle ).getString( key );
			}
			catch ( MissingResourceException e )
			{
				// Fail gracefully: we seem to have problems locating, say,
				// org.jboss.seam.core.SeamResourceBundle?

				return null;
			}
		}

		// No bundle

		else
		{
			return null;
		}

		if ( localizedKey != null )
			return localizedKey;

		return StringUtils.RESOURCE_KEY_NOT_FOUND_PREFIX + key + StringUtils.RESOURCE_KEY_NOT_FOUND_SUFFIX;
	}

	/**
	 * Convenience method for <code>metawidget.xml</code>.
	 * <p>
	 * This method will not override existing, manually specified <code>&lt;f:param /&gt;</code>
	 */

	public void setParameter( String name, Object value )
	{
		UIParameter parameter = FacesUtils.findParameterWithName( this, name );

		if ( parameter != null )
			return;

		FacesContext context = getFacesContext();
		parameter = (UIParameter) context.getApplication().createComponent( "javax.faces.Parameter" );
		parameter.setId( context.getViewRoot().createUniqueId() );
		parameter.setName( name );
		parameter.setValue( value );
		getChildren().add( parameter );
	}

	/**
	 * Storage area for WidgetProcessors, Layouts, and other stateless clients.
	 * <p>
	 * Unlike <code>.setAttribute</code>, these values are not serialized by
	 * <code>ResponseStateManagerImpl</code>.
	 */

	public void putClientProperty( Object key, Object value )
	{
		if ( mClientProperties == null )
			mClientProperties = CollectionUtils.newHashMap();

		mClientProperties.put( key, value );
	}

	/**
	 * Storage area for WidgetProcessors, Layouts, and other stateless clients.
	 * <p>
	 * Unlike <code>.getAttribute</code>, these values are not serialized by
	 * <code>ResponseStateManagerImpl</code>.
	 */

	@SuppressWarnings( "unchecked" )
	public <T> T getClientProperty( Object key )
	{
		if ( mClientProperties == null )
			return null;

		return (T) mClientProperties.get( key );
	}

	@Override
	public void encodeBegin( FacesContext context )
		throws IOException
	{
		try
		{
			// Validation error? Do not rebuild, as we will lose the invalid values in the
			// components. Instead, just move along to our renderer

			if ( context.getMaximumSeverity() != null )
			{
				super.encodeBegin( context );
				return;
			}

			configure();

			// Inspect from the value binding...

			ValueBinding valueBinding = getValueBinding( "value" );

			if ( valueBinding != null )
			{
				mMetawidgetMixin.buildWidgets( inspect( valueBinding, mInspectFromParent ) );
				super.encodeBegin( context );
				return;
			}

			// ...or from a raw value (for jBPM)...

			if ( mValue != null )
			{
				mMetawidgetMixin.buildWidgets( inspect( null, (String) mValue ) );
				super.encodeBegin( context );
				return;
			}

			// ...or run without inspection (eg. using the Metawidget purely for layout)

			mMetawidgetMixin.buildWidgets( null );
			super.encodeBegin( context );
		}
		catch ( Exception e )
		{
			// IOException does not take a Throwable 'cause' argument until Java 6, so
			// as we need to stay 1.4 compatible we output the trace here

			LogUtils.getLog( getClass() ).error( "Unable to encodeBegin", e );

			// At this top level, it is more 'proper' to throw an IOException than
			// a MetawidgetException, as that is what the layers above are expecting

			throw new IOException( e.getMessage() );
		}
	}

	/**
	 * This method is public for use by WidgetBuilders.
	 */

	public String inspect( Object toInspect, String type, String... names )
	{
		return mMetawidgetMixin.inspect( toInspect, type, names );
	}

	protected abstract UIMetawidget buildNestedMetawidget( Map<String, String> attributes );

	protected void initNestedMetawidget( UIMetawidget nestedMetawidget, Map<String, String> attributes )
		throws Exception
	{
		// Don't reconfigure...

		nestedMetawidget.setConfig( null );

		// ...instead, copy runtime values

		mMetawidgetMixin.initNestedMixin( nestedMetawidget.mMetawidgetMixin, attributes );

		// Read-only
		//
		// Note: initNestedMixin takes care of literal values. This is concerned with the value
		// binding

		if ( !TRUE.equals( attributes.get( READ_ONLY ) ) )
		{
			ValueBinding bindingReadOnly = getValueBinding( "readOnly" );

			if ( bindingReadOnly != null )
				nestedMetawidget.setValueBinding( "readOnly", bindingReadOnly );
		}

		// Bundle

		nestedMetawidget.setValueBinding( "bundle", getValueBinding( "bundle" ) );

		// Renderer type

		nestedMetawidget.setRendererType( getRendererType() );

		// Parameters

		FacesUtils.copyParameters( this, nestedMetawidget, "columns" );

		// Note: it is very dangerous to do, say...
		//
		// to.getAttributes().putAll( from.getAttributes() );
		//
		// ...in order to copy all arbitary attributes, because some frameworks (eg. Facelets) use
		// the attributes map as a storage area for special flags (eg.
		// ComponentSupport.MARK_CREATED) that should not get copied down from component to
		// component!
	}

	@Override
	public Object saveState( FacesContext context )
	{
		Object values[] = new Object[5];
		values[0] = super.saveState( context );
		values[1] = mValue;
		values[2] = mReadOnly;
		values[3] = mConfig;
		values[4] = mInspectFromParent;

		return values;
	}

	@Override
	public void restoreState( FacesContext context, Object state )
	{
		Object values[] = (Object[]) state;
		super.restoreState( context, values[0] );

		mValue = values[1];
		mReadOnly = (Boolean) values[2];
		mConfig = (String) values[3];
		mInspectFromParent = (Boolean) values[4];
	}

	//
	// Protected methods
	//

	/**
	 * Instantiate the MetawidgetMixin used by this Metawidget.
	 * <p>
	 * Subclasses wishing to use their own MetawidgetMixin should override this method to
	 * instantiate their version.
	 */

	protected UIMetawidgetMixin newMetawidgetMixin()
	{
		return new UIMetawidgetMixin();
	}

	protected UIMetawidgetMixin getMetawidgetMixin()
	{
		return mMetawidgetMixin;
	}

	/**
	 * Inspect the value binding.
	 * <p>
	 * A value binding is optional. UIMetawidget can be used purely to lay out manually-specified
	 * components
	 */

	protected String inspect( ValueBinding valueBinding, boolean inspectFromParent )
	{
		if ( valueBinding == null )
			return null;

		// Inspect the object directly

		FacesContext context = getFacesContext();
		String valueBindingString = valueBinding.getExpressionString();

		if ( !inspectFromParent || !FacesUtils.isExpression( valueBindingString ) )
		{
			Object toInspect = valueBinding.getValue( context );

			if ( toInspect != null && !ClassUtils.isPrimitiveWrapper( toInspect.getClass() ) )
			{
				Class<?> classToInspect = ClassUtils.getUnproxiedClass( toInspect.getClass() );
				return inspect( toInspect, classToInspect.getName() );
			}
		}

		// In the event the direct object is 'null' or a primitive...

		String binding = FacesUtils.unwrapExpression( valueBindingString );

		// ...and the EL expression is such that we can chop it off to get to the parent...
		//
		// Note: using EL functions in generated ValueExpressions doesn't actually work yet,
		// see https://javaserverfaces.dev.java.net/issues/show_bug.cgi?id=813. A workaround is
		// to assign the function to a temporary, request-scoped variable using c:set

		if ( binding.indexOf( ' ' ) == -1 && binding.indexOf( ':' ) == -1 && binding.indexOf( '(' ) == -1 )
		{
			int lastIndexOf = binding.lastIndexOf( StringUtils.SEPARATOR_DOT_CHAR );

			if ( lastIndexOf != -1 )
			{
				// ...traverse from the parent as there may be useful metadata there (such as 'name'
				// and 'type')

				Application application = context.getApplication();
				ValueBinding bindingParent = application.createValueBinding( FacesUtils.wrapExpression( binding.substring( 0, lastIndexOf ) ) );
				Object toInspect = bindingParent.getValue( context );

				if ( toInspect != null )
				{
					Class<?> classToInspect = ClassUtils.getUnproxiedClass( toInspect.getClass() );
					return inspect( toInspect, classToInspect.getName(), binding.substring( lastIndexOf + 1 ) );
				}
			}
		}

		return null;
	}

	protected void configure()
	{
		if ( !mNeedsConfiguring )
			return;

		mNeedsConfiguring = false;

		FacesContext facesContext = getFacesContext();
		Map<String, Object> applicationMap = facesContext.getExternalContext().getApplicationMap();
		ConfigReader configReader = (ConfigReader) applicationMap.get( APPLICATION_ATTRIBUTE_CONFIG_READER );

		if ( configReader == null )
		{
			configReader = new FacesConfigReader();
			applicationMap.put( APPLICATION_ATTRIBUTE_CONFIG_READER, configReader );
		}

		if ( mConfig != null )
		{
			try
			{
				configReader.configure( mConfig, this );
			}
			catch( MetawidgetException e )
			{
				if ( !DEFAULT_USER_CONFIG.equals( mConfig ) || !( e.getCause() instanceof FileNotFoundException ))
					throw e;

				LogUtils.getLog( UIMetawidget.class ).info( "Could not locate " + DEFAULT_USER_CONFIG + ". This file is optional, but if you HAVE created one then Metawidget isn't finding it!" );
			}
		}


		mMetawidgetMixin.configureDefaults( configReader, getDefaultConfiguration(), UIMetawidget.class );
	}

	protected abstract String getDefaultConfiguration();

	/**
	 * Build child widgets.
	 */

	protected void startBuild()
	{
		// Metawidget has no valueBinding? Won't be destroying/recreating any components, then.
		//
		// This is an optimisation, but is also important for cases like RichFacesLayout, which
		// use nested Metawidgets (without a value binding) purely for layout. They populate a new
		// Metawidget with previously inspected components, and we don't want them destroyed
		// here and/or unnecessarily re-inspected in endBuild

		if ( getValueBinding( "value" ) == null )
			return;

		// Remove any components we created previously (this is
		// important for polymorphic controls, which may change from
		// refresh to refresh)

		List<UIComponent> children = getChildren();

		for ( Iterator<UIComponent> i = children.iterator(); i.hasNext(); )
		{
			UIComponent componentChild = i.next();
			Map<String, Object> attributes = componentChild.getAttributes();

			// The first time in, children will have no metadata attached. Use this opportunity
			// to tag the initial children so that we never recreate them

			if ( !attributes.containsKey( COMPONENT_ATTRIBUTE_METADATA ) )
			{
				attributes.put( COMPONENT_ATTRIBUTE_NOT_RECREATABLE, true );
				continue;
			}

			// Do not remove locked or overridden components...

			if ( attributes.containsKey( COMPONENT_ATTRIBUTE_NOT_RECREATABLE ) )
			{
				// ...but always remove their metadata, otherwise
				// they will not be removed/re-added (and therefore re-ordered) upon POSTback

				attributes.remove( COMPONENT_ATTRIBUTE_METADATA );
				continue;
			}

			i.remove();
		}
	}

	protected UIComponent getOverriddenWidget( String elementName, Map<String, String> attributes )
	{
		// Metawidget has no valueBinding? Not overridable, then

		ValueBinding metawidgetValueBinding = getValueBinding( "value" );

		if ( metawidgetValueBinding == null )
			return null;

		// Actions

		String binding = attributes.get( FACES_EXPRESSION );

		if ( ACTION.equals( elementName ) )
		{
			if ( binding == null )
			{
				String facesExpressionPrefix = FacesUtils.unwrapExpression( metawidgetValueBinding.getExpressionString() );
				binding = FacesUtils.wrapExpression( facesExpressionPrefix + StringUtils.SEPARATOR_DOT_CHAR + attributes.get( NAME ) );
			}

			return FacesUtils.findRenderedComponentWithMethodBinding( UIMetawidget.this, binding );
		}

		// Properties

		if ( binding == null )
		{
			if ( ENTITY.equals( elementName ) )
			{
				binding = metawidgetValueBinding.getExpressionString();
			}
			else
			{
				String facesExpressionPrefix = FacesUtils.unwrapExpression( metawidgetValueBinding.getExpressionString() );
				binding = FacesUtils.wrapExpression( facesExpressionPrefix + StringUtils.SEPARATOR_DOT_CHAR + attributes.get( NAME ) );
			}
		}

		return FacesUtils.findRenderedComponentWithValueBinding( UIMetawidget.this, binding );
	}

	protected void endBuild()
		throws Exception
	{
		List<UIComponent> children = getChildren();

		// Inspect any remaining components, and sort them to the bottom

		for ( int loop = 0, index = 0, length = children.size(); loop < length; loop++ )
		{
			UIComponent component = children.get( index );

			// If this component has already been processed by the inspection (ie. contains
			// metadata), is not rendered, or is a UIParameter, skip it
			//
			// This is also handy for RichFacesLayout, which uses a nested Metawidget purely as a
			// layout tool: it populates a new Metawidget with some previously inspected components.
			// This check makes sure they aren't unnecessarily re-inspected here

			Map<String, Object> miscAttributes = component.getAttributes();

			if ( miscAttributes.containsKey( COMPONENT_ATTRIBUTE_METADATA ) || !component.isRendered() || component instanceof UIParameter )
			{
				index++;
				continue;
			}

			// Try and determine some metadata for the component by inspecting its binding. This
			// helps our layout display proper labels, required stars etc. - even for components
			// whose binding is not a descendant of our parent binding

			Map<String, String> childAttributes = CollectionUtils.newHashMap();
			miscAttributes.put( COMPONENT_ATTRIBUTE_METADATA, childAttributes );

			ValueBinding binding = component.getValueBinding( "value" );

			if ( binding != null )
			{
				String xml = inspect( binding, true );

				if ( xml != null )
				{
					Document document = XmlUtils.documentFromString( xml );
					childAttributes.putAll( XmlUtils.getAttributesAsMap( document.getDocumentElement().getFirstChild() ) );
				}
			}
			else
			{
				// If no found metadata, default to no section.
				//
				// This is so if a user puts, say, a <t:div/> in the component tree, it doesn't
				// appear inside an existing section

				childAttributes.put( SECTION, "" );
			}

			// Stubs

			if ( component instanceof UIStub )
				childAttributes.putAll( ( mMetawidgetMixin ).getStubAttributes( component ) );

			mMetawidgetMixin.getLayout().layoutChild( component, PROPERTY, childAttributes, this );
		}
	}

	//
	// Inner class
	//

	protected class UIMetawidgetMixin
		extends MetawidgetMixin<UIComponent, UIMetawidget>
	{
		//
		// Public methods
		//

		/**
		 * Overriden to just-in-time evaluate EL binding.
		 */

		@Override
		public boolean isReadOnly()
		{
			return UIMetawidget.this.isReadOnly();
		}

		@Override
		public void setReadOnly( boolean readOnly )
		{
			UIMetawidget.this.setReadOnly( readOnly );
		}

		//
		// Protected methods
		//

		@Override
		protected void startBuild()
			throws Exception
		{
			super.startBuild();
			UIMetawidget.this.startBuild();
		}

		@Override
		protected UIComponent getOverriddenWidget( String elementName, Map<String, String> attributes )
		{
			return UIMetawidget.this.getOverriddenWidget( elementName, attributes );
		}

		@Override
		protected boolean isStub( UIComponent widget )
		{
			return ( widget instanceof UIStub );
		}

		@Override
		protected Map<String, String> getStubAttributes( UIComponent stub )
		{
			return ( (UIStub) stub ).getStubAttributes();
		}

		@Override
		protected UIMetawidget buildNestedMetawidget( Map<String, String> attributes )
			throws Exception
		{
			UIMetawidget nestedMetawidget = UIMetawidget.this.buildNestedMetawidget( attributes );
			UIMetawidget.this.initNestedMetawidget( nestedMetawidget, attributes );

			return nestedMetawidget;
		}

		@Override
		protected void addWidget( UIComponent widget, String elementName, Map<String, String> attributes )
			throws Exception
		{
			Map<String, Object> componentAttributes = widget.getAttributes();
			componentAttributes.put( COMPONENT_ATTRIBUTE_METADATA, attributes );

			super.addWidget( widget, elementName, attributes );
		}

		@Override
		protected void endBuild()
			throws Exception
		{
			super.endBuild();
			UIMetawidget.this.endBuild();
		}

		@Override
		protected UIMetawidget getMixinOwner()
		{
			return UIMetawidget.this;
		}

		@Override
		protected MetawidgetMixin<UIComponent, UIMetawidget> getNestedMixin( UIMetawidget metawidget )
		{
			return metawidget.getMetawidgetMixin();
		}
	}
}