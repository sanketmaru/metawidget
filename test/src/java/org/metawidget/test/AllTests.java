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

package org.metawidget.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.metawidget.test.example.ExampleTests;
import org.metawidget.test.faces.FacesUtilsTest;
import org.metawidget.test.faces.UIMetawidgetTest;
import org.metawidget.test.gwt.GwtUtilsTest;
import org.metawidget.test.inspector.InspectorTests;
import org.metawidget.test.mixin.MixinTests;
import org.metawidget.test.swing.SwingMetawidgetTests;
import org.metawidget.test.util.UtilTests;

/**
 * @author Richard Kennard
 */

public class AllTests
	extends TestCase
{
	//
	// Constructor
	//

	/**
	 * JUnit 3.7 constructor.
	 */

	public AllTests( String name )
	{
		super( name );
	}

	//
	// Public statics
	//

	public static Test suite()
	{
		// Note: if this fails saying 'java.lang.RuntimeException: Stub!' or such, it's
		// probably because it is picking up the version of JUnit inside android.jar.
		//
		// To fix, place the junit-4.5.jar first on your CLASSPATH

		TestSuite suite = new TestSuite( "Metawidget Tests" );
		suite.addTest( ExampleTests.suite() );
		suite.addTestSuite( UIMetawidgetTest.class );
		suite.addTestSuite( FacesUtilsTest.class );
		suite.addTestSuite( GwtUtilsTest.class );
		suite.addTest( InspectorTests.suite() );
		suite.addTest( MixinTests.suite() );
		suite.addTest( SwingMetawidgetTests.suite() );
		suite.addTest( UtilTests.suite() );

		return suite;
	}
}